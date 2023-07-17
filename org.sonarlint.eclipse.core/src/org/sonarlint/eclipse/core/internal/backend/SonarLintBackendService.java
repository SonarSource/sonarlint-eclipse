/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2023 SonarSource SA
 * sonarlint@sonarsource.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.core.internal.backend;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.StoragePathManager;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.internal.vcs.VcsService;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.SonarLintBackendImpl;
import org.sonarsource.sonarlint.core.clientapi.SonarLintBackend;
import org.sonarsource.sonarlint.core.clientapi.SonarLintClient;
import org.sonarsource.sonarlint.core.clientapi.backend.initialize.ClientInfoDto;
import org.sonarsource.sonarlint.core.clientapi.backend.initialize.FeatureFlagsDto;
import org.sonarsource.sonarlint.core.clientapi.backend.initialize.InitializeParams;
import org.sonarsource.sonarlint.core.clientapi.backend.issue.AddIssueCommentParams;
import org.sonarsource.sonarlint.core.clientapi.backend.issue.ChangeIssueStatusParams;
import org.sonarsource.sonarlint.core.clientapi.backend.issue.IssueStatus;
import org.sonarsource.sonarlint.core.clientapi.backend.rules.GetEffectiveRuleDetailsParams;
import org.sonarsource.sonarlint.core.clientapi.backend.rules.GetEffectiveRuleDetailsResponse;
import org.sonarsource.sonarlint.core.clientapi.backend.rules.GetStandaloneRuleDescriptionParams;
import org.sonarsource.sonarlint.core.clientapi.backend.rules.GetStandaloneRuleDescriptionResponse;
import org.sonarsource.sonarlint.core.commons.Language;

import static java.util.Objects.requireNonNull;
import static org.sonarlint.eclipse.core.internal.utils.StringUtils.defaultString;

public class SonarLintBackendService {

  private static final SonarLintBackendService INSTANCE = new SonarLintBackendService();

  @Nullable
  private static ConfigScopeSynchronizer configScopeSynchronizer;
  @Nullable
  private static ConnectionSynchronizer connectionSynchronizer;
  @Nullable
  private SonarLintBackend backend;

  private Job initJob;

  public static SonarLintBackendService get() {
    return INSTANCE;
  }

  public synchronized void init(SonarLintClient client) {
    if (backend != null) {
      throw new IllegalStateException("Backend is already initialized");
    }

    initJob = new Job("Backend initialization") {

      @Override
      protected IStatus run(IProgressMonitor monitor) {
        SonarLintLogger.get().debug("Initializing SonarLint backend...");

        backend = new SonarLintBackendImpl(client);

        var embeddedPluginPaths = PluginPathHelper.getEmbeddedPluginPaths();
        embeddedPluginPaths.stream().forEach(p -> SonarLintLogger.get().debug("  - " + p));

        Map<String, Path> embeddedPlugins = new HashMap<>();
        embeddedPlugins.put(Language.JS.getPluginKey(), requireNonNull(PluginPathHelper.findEmbeddedJsPlugin(), "JS/TS plugin not found"));
        embeddedPlugins.put(Language.HTML.getPluginKey(), requireNonNull(PluginPathHelper.findEmbeddedHtmlPlugin(), "HTML plugin not found"));
        embeddedPlugins.put(Language.XML.getPluginKey(), requireNonNull(PluginPathHelper.findEmbeddedXmlPlugin(), "XML plugin not found"));
        embeddedPlugins.put(Language.SECRETS.getPluginKey(), requireNonNull(PluginPathHelper.findEmbeddedSecretsPlugin(), "Secrets plugin not found"));

        var sqConnections = ConnectionSynchronizer.buildSqConnectionDtos();
        var scConnections = ConnectionSynchronizer.buildScConnectionDtos();

        try {
          backend.initialize(new InitializeParams(
            new ClientInfoDto(getIdeName(), "eclipse", "SonarLint Eclipse " + SonarLintUtils.getPluginVersion()),
            new FeatureFlagsDto(true, true, true, true, false),
            StoragePathManager.getStorageDir(),
            StoragePathManager.getDefaultWorkDir(),
            Set.copyOf(embeddedPluginPaths),
            embeddedPlugins,
            SonarLintUtils.getEnabledLanguages(),
            SonarLintUtils.getEnabledLanguages(),
            sqConnections,
            scConnections,
            null,
            SonarLintGlobalConfiguration.buildStandaloneRulesConfig())).get();
        } catch (InterruptedException | ExecutionException e) {
          throw new IllegalStateException("Unable to initialize the SonarLint Backend", e);
        }
        connectionSynchronizer = new ConnectionSynchronizer(backend);
        SonarLintCorePlugin.getServersManager().addServerLifecycleListener(connectionSynchronizer);

        configScopeSynchronizer = new ConfigScopeSynchronizer(backend);
        ResourcesPlugin.getWorkspace().addResourceChangeListener(configScopeSynchronizer);

        configScopeSynchronizer.init();

        VcsService.installBranchChangeListener();

        return Status.OK_STATUS;
      }
    };
    initJob.schedule();

  }

  /** Provide the Backend with the information on a changed VCS branch for further actions, e.g. synchronizing with SQ / SC */
  public void branchChanged(ISonarLintProject project, String newActiveBranchName) {
    configScopeSynchronizer.branchChanged(project, newActiveBranchName);
  }

  public SonarLintBackend getBackend() {
    try {
      requireNonNull(initJob, "SonarLintBackendService has not been initialized").join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      Platform.getLog(SonarLintBackendService.class).error("Interrupted!", e);
    }
    return requireNonNull(backend, "SonarLintBackendService has not been initialized");
  }

  private static String getIdeName() {
    var ideName = "Eclipse";
    var product = Platform.getProduct();
    if (product != null) {
      ideName = defaultString(product.getName(), "Eclipse");
    }
    return ideName;
  }

  /** Get the rules details (global configuration) */
  public GetStandaloneRuleDescriptionResponse getStandaloneRuleDetails(String ruleKey) throws InterruptedException, ExecutionException {
    return getBackend()
      .getRulesService()
      .getStandaloneRuleDetails(new GetStandaloneRuleDescriptionParams(ruleKey))
      .get();
  }

  /** Get the rules details (project configuration, maybe connected mode) */
  public GetEffectiveRuleDetailsResponse getEffectiveRuleDetails(ISonarLintProject project, String ruleKey, @Nullable String contextKey)
    throws InterruptedException, ExecutionException {
    return getBackend()
      .getRulesService()
      .getEffectiveRuleDetails(new GetEffectiveRuleDetailsParams(ConfigScopeSynchronizer.getConfigScopeId(project), ruleKey, contextKey))
      .get();
  }

  public synchronized void stop() {
    VcsService.removeBranchChangeListener();
    if (configScopeSynchronizer != null) {
      ResourcesPlugin.getWorkspace().removeResourceChangeListener(configScopeSynchronizer);
      configScopeSynchronizer = null;
    }
    if (connectionSynchronizer != null) {
      SonarLintCorePlugin.getServersManager().removeServerLifecycleListener(connectionSynchronizer);
      connectionSynchronizer = null;
    }
    var backendLocalCopy = backend;
    backend = null;
    if (backendLocalCopy != null) {
      try {
        backendLocalCopy.shutdown().get(10, TimeUnit.SECONDS);
      } catch (ExecutionException | TimeoutException e) {
        Platform.getLog(SonarLintBackendService.class).error("Unable to stop the SonartLint backend", e);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public CompletableFuture<Void> changeIssueStatus(ISonarLintProject project, String serverIssueKey, IssueStatus newStatus, boolean isTaint) {
    return getBackend()
      .getIssueService()
      .changeStatus(new ChangeIssueStatusParams(ConfigScopeSynchronizer.getConfigScopeId(project), serverIssueKey, newStatus, isTaint));
  }

  public CompletableFuture<Void> addIssueComment(ISonarLintProject project, String serverIssueKey, String text) {
    return getBackend()
      .getIssueService()
      .addComment(new AddIssueCommentParams(ConfigScopeSynchronizer.getConfigScopeId(project), serverIssueKey, text));
  }

}
