/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.IDocument;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.StoragePathManager;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectionFacade;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.internal.telemetry.SonarLintTelemetry;
import org.sonarlint.eclipse.core.internal.utils.DurationUtils;
import org.sonarlint.eclipse.core.internal.utils.JavaRuntimeUtils;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.internal.vcs.VcsService;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.rpc.client.SloopLauncher;
import org.sonarsource.sonarlint.core.rpc.client.SonarLintRpcClientDelegate;
import org.sonarsource.sonarlint.core.rpc.protocol.SonarLintRpcServer;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.analysis.AnalyzeFilesParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.analysis.AnalyzeFilesResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.binding.GetSharedConnectedModeConfigFileParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.binding.GetSharedConnectedModeConfigFileResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.branch.DidVcsRepositoryChangeParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.connection.config.DidChangeCredentialsParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.connection.projects.GetAllProjectsParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.connection.projects.SonarProjectDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.file.GetFilesStatusParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.file.GetFilesStatusResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.ClientConstantInfoDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.FeatureFlagsDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.HttpConfigurationDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.InitializeParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.LanguageSpecificRequirements;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.SonarCloudAlternativeEnvironmentDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.SslConfigurationDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.TelemetryClientConstantAttributesDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.issue.AddIssueCommentParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.issue.ChangeIssueStatusParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.issue.CheckAnticipatedStatusChangeSupportedParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.issue.CheckAnticipatedStatusChangeSupportedResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.issue.ReopenIssueParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.issue.ReopenIssueResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.issue.ResolutionStatus;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.newcode.GetNewCodeDefinitionParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.newcode.GetNewCodeDefinitionResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.GetEffectiveRuleDetailsParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.GetEffectiveRuleDetailsResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.GetStandaloneRuleDescriptionParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.GetStandaloneRuleDescriptionResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.ListAllStandaloneRulesDefinitionsResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.tracking.ClientTrackedFindingDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.tracking.ListAllParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.tracking.ListAllResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.tracking.TrackWithServerIssuesParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.tracking.TrackWithServerIssuesResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.common.Language;

import static java.util.Objects.requireNonNull;
import static org.sonarlint.eclipse.core.internal.utils.StringUtils.defaultString;

public class SonarLintBackendService {

  private static final SonarLintBackendService INSTANCE = new SonarLintBackendService();

  @Nullable
  private static ConfigScopeSynchronizer configScopeSynchronizer;
  @Nullable
  private static ConnectionSynchronizer connectionSynchronizer;
  @Nullable
  private static FileSystemSynchronizer fileSystemSynchronizer;
  @Nullable
  private SonarLintRpcServer backend;

  private Job initJob;

  private SloopLauncher sloopLauncher;

  public static SonarLintBackendService get() {
    return INSTANCE;
  }

  public synchronized void init(SonarLintRpcClientDelegate client) {
    if (sloopLauncher != null) {
      throw new IllegalStateException("Backend is already initialized");
    }
    sloopLauncher = new SloopLauncher(client);

    initJob = new Job("Backend initialization") {

      @Override
      protected IStatus run(IProgressMonitor monitor) {
        SonarLintLogger.get().debug("Initializing SonarLint backend...");
        try {
          var sloopJarUrls = SonarLintCorePlugin.getInstance().getBundle().findEntries("/sloop/lib", "sonarlint-core-*", false);
          if (!sloopJarUrls.hasMoreElements()) {
            throw new IllegalStateException("Unable to locate the Sloop installation");
          }
          var sloopJarUrl = FileLocator.toFileURL(sloopJarUrls.nextElement());
          var sloopJarPath = new File(sloopJarUrl.getFile()).toPath();
          SonarLintLogger.get().debug("SonarLint Core Jar archive located at " + sloopJarPath);
          var sloopBasedir = sloopJarPath.getParent().getParent();
          SonarLintLogger.get().debug("Sloop located in " + sloopBasedir);

          var javaRuntimeInformation = JavaRuntimeUtils.getJavaRuntime();
          var javaRuntimePath = javaRuntimeInformation.getPath();
          switch (javaRuntimeInformation.getProvider()) {
            case SELF_MANAGED:
              SonarLintLogger.get().info("Using self-managed Java installation");
              fixExecutablePermissions();
              break;
            case ECLIPSE_MANAGED:
              SonarLintLogger.get().info("Using Java installation of Eclipse");
              break;
            case SONARLINT_BUNDLED:
              SonarLintLogger.get().info("Using Java installation of SonarLint");
          }

          var sloop = sloopLauncher.start(sloopBasedir, javaRuntimePath);
          sloop.onExit().thenAccept(SonarLintBackendService::onSloopExit);
          backend = sloop.getRpcServer();

          var embeddedPluginPaths = PluginPathHelper.getEmbeddedPluginPaths();
          embeddedPluginPaths.stream().forEach(p -> SonarLintLogger.get().debug("  - " + p));

          Map<String, Path> embeddedPlugins = new HashMap<>();
          embeddedPlugins.put("javascript", requireNonNull(PluginPathHelper.findEmbeddedJsPlugin(), "JS/TS plugin not found"));
          embeddedPlugins.put("web", requireNonNull(PluginPathHelper.findEmbeddedHtmlPlugin(), "HTML plugin not found"));
          embeddedPlugins.put("xml", requireNonNull(PluginPathHelper.findEmbeddedXmlPlugin(), "XML plugin not found"));
          embeddedPlugins.put("text", requireNonNull(PluginPathHelper.findEmbeddedSecretsPlugin(), "Secrets plugin not found"));

          var sqConnections = ConnectionSynchronizer.buildSqConnectionDtos();
          var scConnections = ConnectionSynchronizer.buildScConnectionDtos();

          // Check if telemetry was disabled via system properties (e.g. in unit / integration tests)
          var telemetryEnabled = !Boolean.parseBoolean(System.getProperty("sonarlint.telemetry.disabled", "false"));

          backend.initialize(new InitializeParams(
            new ClientConstantInfoDto(getIdeName(), "SonarLint Eclipse " + SonarLintUtils.getPluginVersion(), SonarLintUtils.getPlatformPid()),
            new TelemetryClientConstantAttributesDto("eclipse", "SonarLint Eclipse", SonarLintUtils.getPluginVersion(), SonarLintTelemetry.ideVersionForTelemetry(),
              Map.of()),
            getHttpConfiguration(),
            getSonarCloudAlternativeEnvironment(),
            new FeatureFlagsDto(true, true, true, true, false, true, false, true, telemetryEnabled),
            StoragePathManager.getStorageDir(),
            StoragePathManager.getDefaultWorkDir(),
            Set.copyOf(embeddedPluginPaths),
            embeddedPlugins,
            SonarLintUtils.getStandaloneEnabledLanguages().stream().map(l -> Language.valueOf(l.name())).collect(Collectors.toSet()),
            SonarLintUtils.getConnectedEnabledLanguages().stream().map(l -> Language.valueOf(l.name())).collect(Collectors.toSet()),
            sqConnections,
            scConnections,
            null,
            SonarLintGlobalConfiguration.buildStandaloneRulesConfigDto(),
            SonarLintGlobalConfiguration.PREF_ISSUE_PERIOD_NEWCODE.equals(SonarLintGlobalConfiguration.getIssuePeriod()),
            new LanguageSpecificRequirements(SonarLintGlobalConfiguration.getNodejsPath(), null))).join();
        } catch (IOException e) {
          throw new IllegalStateException("Unable to initialize the SonarLint Backend", e);
        }
        connectionSynchronizer = new ConnectionSynchronizer(backend);
        SonarLintCorePlugin.getConnectionManager().addConnectionManagerListener(connectionSynchronizer);

        configScopeSynchronizer = new ConfigScopeSynchronizer(backend);
        ResourcesPlugin.getWorkspace().addResourceChangeListener(configScopeSynchronizer);
        configScopeSynchronizer.init();

        fileSystemSynchronizer = new FileSystemSynchronizer(backend);
        ResourcesPlugin.getWorkspace().addResourceChangeListener(fileSystemSynchronizer, IResourceChangeEvent.POST_CHANGE);

        VcsService.installBranchChangeListener();

        SonarLintRpcClientSupportSynchronizer.setSloopAvailability(true);

        return Status.OK_STATUS;
      }

      /**
       * When running tests, Tycho does not execute p2 directives, so we have to manually fix the permissions of the sloop executables.
       */
      private void fixExecutablePermissions() throws IOException {
        fixExecutablePermissions("/sloop/jre/bin", "java");
        fixExecutablePermissions("/sloop/jre/lib", "jspawnhelper");
        fixExecutablePermissions("/sloop/jre/lib", "jexec");
      }

      private void fixExecutablePermissions(String dir, String file) throws IOException {
        var sloopShellScriptUrls = SonarLintCorePlugin.getInstance().getBundle().findEntries(dir, file, false);
        if (sloopShellScriptUrls != null && sloopShellScriptUrls.hasMoreElements()) {
          var jreBin = FileLocator.toFileURL(sloopShellScriptUrls.nextElement());
          var jreBinPath = new File(jreBin.getFile()).toPath();
          var existingPerm = Files.getPosixFilePermissions(jreBinPath);
          if (!existingPerm.contains(PosixFilePermission.OWNER_EXECUTE)) {
            existingPerm.add(PosixFilePermission.OWNER_EXECUTE);
            Files.setPosixFilePermissions(jreBinPath, existingPerm);
          }
        }
      }

    };
    initJob.schedule();

  }

  private static HttpConfigurationDto getHttpConfiguration() {
    return new HttpConfigurationDto(
      new SslConfigurationDto(getPathProperty("sonarlint.ssl.trustStorePath"),
        System.getProperty("sonarlint.ssl.trustStorePassword"),
        System.getProperty("sonarlint.ssl.trustStoreType"),
        getPathProperty("sonarlint.ssl.keyStorePath"),
        System.getProperty("sonarlint.ssl.keyStorePassword"),
        System.getProperty("sonarlint.ssl.keyStoreType")),
      DurationUtils.getTimeoutProperty("sonarlint.http.connectTimeout"),
      DurationUtils.getTimeoutProperty("sonarlint.http.socketTimeout"),
      DurationUtils.getTimeoutProperty("sonarlint.http.connectionRequestTimeout"),
      DurationUtils.getTimeoutProperty("sonarlint.http.responseTimeout"));
  }

  @Nullable
  private static SonarCloudAlternativeEnvironmentDto getSonarCloudAlternativeEnvironment() {
    var sonarCloudUrl = System.getProperty("sonarlint.internal.sonarcloud.url");
    var sonarCloudWebSocketUrl = System.getProperty("sonarlint.internal.sonarcloud.websocket.url");
    if (sonarCloudUrl != null && sonarCloudWebSocketUrl != null) {
      return new SonarCloudAlternativeEnvironmentDto(URI.create(sonarCloudUrl), URI.create(sonarCloudWebSocketUrl));
    }
    return null;
  }

  @Nullable
  private static Path getPathProperty(String propertyName) {
    var property = System.getProperty(propertyName);
    return property == null ? null : Paths.get(property);
  }

  private static void onSloopExit(int exitCode) {
    if (exitCode != 0) {
      // When the exit code is 0 we accept it as a normal shutdown of the RPC server.
      SonarLintRpcClientSupportSynchronizer.setSloopAvailability(false);

      // INFO: With SLE-812 improve this to restart the backend / offer users the possibility to to so!
    }
  }

  /**
   * Inform the backend that VCS changed in a way that may affect the matched SonarProject branch
   */
  public void didVcsRepositoryChange(ISonarLintProject project) {
    getBackend()
      .getSonarProjectBranchService()
      .didVcsRepositoryChange(new DidVcsRepositoryChangeParams(ConfigScopeSynchronizer.getConfigScopeId(project)));
  }

  public void credentialsChanged(ConnectionFacade connection) {
    getBackend().getConnectionService().didChangeCredentials(new DidChangeCredentialsParams(connection.getId()));
  }

  public SonarLintRpcServer getBackend() {
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

  /** Get all the rules available in standalone mode */
  public CompletableFuture<ListAllStandaloneRulesDefinitionsResponse> getStandaloneRules() {
    return getBackend().getRulesService().listAllStandaloneRulesDefinitions();
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
    if (fileSystemSynchronizer != null) {
      ResourcesPlugin.getWorkspace().removeResourceChangeListener(fileSystemSynchronizer);
      fileSystemSynchronizer = null;
    }
    if (configScopeSynchronizer != null) {
      ResourcesPlugin.getWorkspace().removeResourceChangeListener(configScopeSynchronizer);
      configScopeSynchronizer = null;
    }
    if (connectionSynchronizer != null) {
      SonarLintCorePlugin.getConnectionManager().removeConnectionManagerListener(connectionSynchronizer);
      connectionSynchronizer = null;
    }
    if (backend != null) {
      backend.shutdown();
    }
    backend = null;
  }

  /**
   *  INFO: For anticipated issues the `serverIssueKey` parameter has to be replaced with the string representation of
   *        the `TrackedIssue.id` field. This is due to SLCORE using the same method for server and anticipated issues,
   *        therefore the naming confusion!
   */
  public CompletableFuture<Void> changeIssueStatus(ISonarLintProject project, String serverIssueKey, ResolutionStatus newStatus, boolean isTaint) {
    return getBackend()
      .getIssueService()
      .changeStatus(new ChangeIssueStatusParams(ConfigScopeSynchronizer.getConfigScopeId(project), serverIssueKey, newStatus, isTaint));
  }

  /**
   *  INFO: For anticipated issues the `serverIssueKey` parameter has to be replaced with the string representation of
   *        the `TrackedIssue.id` field. This is due to SLCORE using the same method for server and anticipated issues,
   *        therefore the naming confusion!
   */
  public CompletableFuture<Void> addIssueComment(ISonarLintProject project, String serverIssueKey, String text) {
    return getBackend()
      .getIssueService()
      .addComment(new AddIssueCommentParams(ConfigScopeSynchronizer.getConfigScopeId(project), serverIssueKey, text));
  }

  public CompletableFuture<TrackWithServerIssuesResponse> trackWithServerIssues(ISonarLintProject project,
    Map<Path, List<ClientTrackedFindingDto>> clientTrackedIssuesByIdeRelativePath,
    boolean shouldFetchIssuesFromServer) {
    return getBackend().getIssueTrackingService().trackWithServerIssues(
      new TrackWithServerIssuesParams(ConfigScopeSynchronizer.getConfigScopeId(project), clientTrackedIssuesByIdeRelativePath, shouldFetchIssuesFromServer));
  }

  public CompletableFuture<GetNewCodeDefinitionResponse> getNewCodeDefinition(ISonarLintProject project) {
    return getBackend().getNewCodeService().getNewCodeDefinition(new GetNewCodeDefinitionParams(ConfigScopeSynchronizer.getConfigScopeId(project)));
  }

  public CompletableFuture<ReopenIssueResponse> reopenIssue(ISonarLintProject project, String issueKey, Boolean isTaintVulnerability) {
    return getBackend()
      .getIssueService()
      .reopenIssue(new ReopenIssueParams(ConfigScopeSynchronizer.getConfigScopeId(project), issueKey, isTaintVulnerability));
  }

  public CompletableFuture<CheckAnticipatedStatusChangeSupportedResponse> checkAnticipatedStatusChangeSupported(ISonarLintProject project) {
    return getBackend()
      .getIssueService()
      .checkAnticipatedStatusChangeSupported(new CheckAnticipatedStatusChangeSupportedParams(ConfigScopeSynchronizer.getConfigScopeId(project)));
  }

  public CompletableFuture<List<SonarProjectDto>> getAllProjects(ConnectionFacade connectionFacade) {
    return getBackend().getConnectionService().getAllProjects(new GetAllProjectsParams(connectionFacade.toTransientDto())).thenApply(r -> r.getSonarProjects());
  }

  public CompletableFuture<ListAllResponse> listAllTaintVulnerabilities(ISonarLintProject project) {
    return getBackend().getTaintVulnerabilityTrackingService().listAll(new ListAllParams(ConfigScopeSynchronizer.getConfigScopeId(project)));
  }

  public CompletableFuture<GetFilesStatusResponse> getFilesStatus(ISonarLintProject project, Collection<ISonarLintFile> notExcluded) {
    return getBackend().getFileService().getFilesStatus(
      new GetFilesStatusParams(Map.of(ConfigScopeSynchronizer.getConfigScopeId(project), notExcluded.stream().map(ISonarLintFile::uri).collect(Collectors.toList()))));
  }

  public CompletableFuture<GetSharedConnectedModeConfigFileResponse> getSharedConnectedModeConfigFileContents(ISonarLintProject project) {
    return getBackend()
      .getBindingService()
      .getSharedConnectedModeConfigFileContents(new GetSharedConnectedModeConfigFileParams(ConfigScopeSynchronizer.getConfigScopeId(project)));
  }

  public CompletableFuture<AnalyzeFilesResponse> analyzeFiles(ISonarLintProject project, UUID analysisId, Map<ISonarLintFile, IDocument> docPerFiles,
    Map<String, String> extraProps, long startTime) {
    var fileUris = docPerFiles.keySet().stream().map(file -> file.getResource().getLocationURI()).collect(Collectors.toList());
    return getBackend().getAnalysisService().analyzeFiles(new AnalyzeFilesParams(ConfigScopeSynchronizer.getConfigScopeId(project), analysisId, fileUris, extraProps, startTime));
  }
}
