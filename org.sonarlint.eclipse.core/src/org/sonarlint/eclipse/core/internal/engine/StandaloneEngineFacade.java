/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2021 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.engine;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.jobs.SonarLintAnalyzerLogOutput;
import org.sonarlint.eclipse.core.internal.jobs.WrappedProgressMonitor;
import org.sonarlint.eclipse.core.internal.utils.NodeJsManager;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarsource.sonarlint.core.StandaloneSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.client.api.common.Language;
import org.sonarsource.sonarlint.core.client.api.common.RuleDetails;
import org.sonarsource.sonarlint.core.client.api.common.analysis.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.analysis.IssueListener;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneGlobalConfiguration.Builder;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneRuleDetails;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneSonarLintEngine;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

public class StandaloneEngineFacade {

  private StandaloneSonarLintEngine wrappedEngine;

  @Nullable
  private synchronized StandaloneSonarLintEngine getOrCreateEngine() {
    if (wrappedEngine == null) {
      SonarLintLogger.get().info("Starting standalone SonarLint engine " + SonarLintUtils.getPluginVersion() + "...");
      Enumeration<URL> pluginEntriesEnum = SonarLintCorePlugin.getInstance().getBundle().findEntries("/plugins", "*.jar", false);
      if (pluginEntriesEnum != null) {
        List<URL> pluginEntries = Collections.list(pluginEntriesEnum);
        SonarLintLogger.get().debug("Loading embedded analyzers...");
        pluginEntries.stream().forEach(e -> SonarLintLogger.get().debug("  - " + e.getFile()));
        NodeJsManager nodeJsManager = SonarLintCorePlugin.getNodeJsManager();
        Builder builder = StandaloneGlobalConfiguration.builder()
          .addPlugins(pluginEntries.toArray(new URL[0]))
          .setWorkDir(ResourcesPlugin.getWorkspace().getRoot().getLocation().append(".sonarlint").append("default").toFile().toPath())
          .setLogOutput(new SonarLintAnalyzerLogOutput())
          .addEnabledLanguages(SonarLintUtils.getEnabledLanguages().toArray(new Language[0]))
          .setNodeJs(nodeJsManager.getNodeJsPath(), nodeJsManager.getNodeJsVersion());
        SonarLintUtils.getPlatformPid().ifPresent(builder::setClientPid);
        StandaloneGlobalConfiguration globalConfig = builder.build();
        try {
          wrappedEngine = new StandaloneSonarLintEngineImpl(globalConfig);
          SkippedPluginsNotifier.notifyForSkippedPlugins(wrappedEngine.getPluginDetails(), null);
        } catch (Throwable e) {
          SonarLintLogger.get().error("Unable to start standalone SonarLint engine", e);
          wrappedEngine = null;
        }
      } else {
        throw new IllegalStateException("Unable to find any embedded plugin");
      }
    }
    return wrappedEngine;
  }

  private <G> Optional<G> withEngine(Function<StandaloneSonarLintEngine, G> function) {
    getOrCreateEngine();
    synchronized (this) {
      if (wrappedEngine != null) {
        return Optional.ofNullable(function.apply(wrappedEngine));
      }
    }
    return Optional.empty();
  }

  @Nullable
  public AnalysisResults runAnalysis(StandaloneAnalysisConfiguration config, IssueListener issueListener, IProgressMonitor monitor) {
    return withEngine(engine -> {
      AnalysisResults analysisResults = engine.analyze(config, issueListener, null, new WrappedProgressMonitor(monitor, "Analysis"));
      AnalysisRequirementNotifications.notifyOnceForSkippedPlugins(analysisResults, engine.getPluginDetails());
      return analysisResults;
    }).orElse(null);
  }

  @Nullable
  public RuleDetails getRuleDescription(String ruleKey) {
    return withEngine(engine -> engine.getRuleDetails(ruleKey).orElse(null)).orElse(null);
  }

  public Collection<StandaloneRuleDetails> getAllRuleDetails() {
    return withEngine(engine -> engine.getAllRuleDetails()
      .stream()
      .filter(r -> r.getLanguage() != Language.TS)
      .collect(toSet()))
        .orElse(emptySet());
  }

  public synchronized void stop() {
    if (wrappedEngine != null) {
      wrappedEngine.stop();
      wrappedEngine = null;
    }
  }

}
