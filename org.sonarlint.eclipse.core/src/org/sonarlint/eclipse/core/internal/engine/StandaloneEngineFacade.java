/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2022 SonarSource SA
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

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.backend.PluginPathHelper;
import org.sonarlint.eclipse.core.internal.jobs.SonarLintAnalyzerLogOutput;
import org.sonarlint.eclipse.core.internal.jobs.WrappedProgressMonitor;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarsource.sonarlint.core.StandaloneSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.analysis.api.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.RuleDetails;
import org.sonarsource.sonarlint.core.client.api.common.analysis.IssueListener;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneRuleDetails;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneSonarLintEngine;
import org.sonarsource.sonarlint.core.commons.Language;

import static java.util.Collections.emptySet;

public class StandaloneEngineFacade {

  @Nullable
  private StandaloneSonarLintEngine wrappedEngine;

  @Nullable
  private synchronized StandaloneSonarLintEngine getOrCreateEngine() {
    if (wrappedEngine == null) {
      SonarLintLogger.get().info("Starting standalone SonarLint engine " + SonarLintUtils.getPluginVersion() + "...");
      var nodeJsManager = SonarLintCorePlugin.getNodeJsManager();
      var globalConfig = StandaloneGlobalConfiguration.builder()
        .addPlugins(PluginPathHelper.getEmbeddedPluginPaths().stream().toArray(Path[]::new))
        .setWorkDir(ResourcesPlugin.getWorkspace().getRoot().getLocation().append(".sonarlint").append("default").toFile().toPath())
        .setLogOutput(new SonarLintAnalyzerLogOutput())
        .addEnabledLanguages(SonarLintUtils.getEnabledLanguages().toArray(new Language[0]))
        .setNodeJs(nodeJsManager.getNodeJsPath(), nodeJsManager.getNodeJsVersion())
        .setClientPid(SonarLintUtils.getPlatformPid()).build();
      try {
        wrappedEngine = new StandaloneSonarLintEngineImpl(globalConfig);
        SkippedPluginsNotifier.notifyForSkippedPlugins(wrappedEngine.getPluginDetails(), null);
      } catch (Throwable e) {
        SonarLintLogger.get().error("Unable to start standalone SonarLint engine", e);
        wrappedEngine = null;
      }
    }
    return wrappedEngine;
  }

  private <G> Optional<G> withEngine(Function<StandaloneSonarLintEngine, G> function) {
    getOrCreateEngine();
    if (wrappedEngine != null) {
      return Optional.ofNullable(function.apply(wrappedEngine));
    }
    return Optional.empty();
  }

  @Nullable
  public AnalysisResults runAnalysis(StandaloneAnalysisConfiguration config, IssueListener issueListener, IProgressMonitor monitor) {
    return withEngine(engine -> {
      var analysisResults = engine.analyze(config, issueListener, null, new WrappedProgressMonitor(monitor, "Analysis"));
      AnalysisRequirementNotifications.notifyOnceForSkippedPlugins(analysisResults, engine.getPluginDetails());
      return analysisResults;
    }).orElseThrow(() -> new IllegalStateException("SonarLint Engine not available"));
  }

  @Nullable
  public RuleDetails getRuleDescription(String ruleKey) {
    return withEngine(engine -> engine.getRuleDetails(ruleKey).orElse(null)).orElse(null);
  }

  public Collection<StandaloneRuleDetails> getAllRuleDetails() {
    return withEngine(StandaloneSonarLintEngine::getAllRuleDetails)
      .orElse(emptySet());
  }

  public synchronized void stop() {
    if (wrappedEngine != null) {
      wrappedEngine.stop();
      wrappedEngine = null;
    }
  }

}
