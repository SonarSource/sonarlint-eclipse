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
package org.sonarlint.eclipse.core.internal.engine;

import java.util.Optional;
import java.util.function.Function;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.StoragePathManager;
import org.sonarlint.eclipse.core.internal.backend.ConfigScopeSynchronizer;
import org.sonarlint.eclipse.core.internal.backend.SonarLintBackendService;
import org.sonarlint.eclipse.core.internal.jobs.SonarLintUtilsLogOutput;
import org.sonarlint.eclipse.core.internal.jobs.WrappedProgressMonitor;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.analysis.api.AnalysisResults;
import org.sonarsource.sonarlint.core.client.legacy.analysis.AnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.legacy.analysis.EngineConfiguration;
import org.sonarsource.sonarlint.core.client.legacy.analysis.RawIssueListener;
import org.sonarsource.sonarlint.core.client.legacy.analysis.SonarLintAnalysisEngine;

public class StandaloneEngineFacade {
  @Nullable
  private SonarLintAnalysisEngine wrappedEngine;

  @Nullable
  private synchronized SonarLintAnalysisEngine getOrCreateEngine() {
    if (wrappedEngine == null) {
      SonarLintLogger.get().info("Starting standalone SonarLint engine " + SonarLintUtils.getPluginVersion() + "...");
      var globalConfig = EngineConfiguration.builder()
        .setWorkDir(StoragePathManager.getDefaultWorkDir())
        .setLogOutput(new SonarLintUtilsLogOutput())
        .setClientPid(SonarLintUtils.getPlatformPid()).build();
      try {
        wrappedEngine = new SonarLintAnalysisEngine(globalConfig, SonarLintBackendService.get().getBackend(), null);
        SkippedPluginsNotifier.notifyForSkippedPlugins(wrappedEngine.getPluginDetails(), null);
      } catch (Throwable e) {
        SonarLintLogger.get().error("Unable to start standalone SonarLint engine", e);
        wrappedEngine = null;
      }
    }
    return wrappedEngine;
  }

  private <G> Optional<G> withEngine(Function<SonarLintAnalysisEngine, G> function) {
    getOrCreateEngine();
    if (wrappedEngine != null) {
      return Optional.ofNullable(function.apply(wrappedEngine));
    }
    return Optional.empty();
  }

  public AnalysisResults runAnalysis(ISonarLintProject project, AnalysisConfiguration config, RawIssueListener issueListener, IProgressMonitor monitor) {
    var configScopeId = ConfigScopeSynchronizer.getConfigScopeId(project);
    return withEngine(engine -> {
      var analysisResults = engine.analyze(config, issueListener, null, new WrappedProgressMonitor(monitor, "Analysis"), configScopeId);
      AnalysisRequirementNotifications.notifyOnceForSkippedPlugins(analysisResults, engine.getPluginDetails());
      return analysisResults;
    }).orElseThrow(() -> new IllegalStateException("SonarLint Engine not available"));
  }

  public synchronized void stop() {
    if (wrappedEngine != null) {
      wrappedEngine.stop();
      wrappedEngine = null;
    }
  }
}
