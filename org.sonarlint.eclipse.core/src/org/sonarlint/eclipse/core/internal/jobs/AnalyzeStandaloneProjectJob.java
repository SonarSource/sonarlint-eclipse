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
package org.sonarlint.eclipse.core.internal.jobs;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.eclipse.core.runtime.IProgressMonitor;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarsource.sonarlint.core.analysis.api.AnalysisResults;
import org.sonarsource.sonarlint.core.analysis.api.ClientInputFile;
import org.sonarsource.sonarlint.core.client.legacy.analysis.AnalysisConfiguration;

public class AnalyzeStandaloneProjectJob extends AbstractAnalyzeProjectJob {

  public AnalyzeStandaloneProjectJob(AnalyzeProjectRequest request) {
    super(request);
  }

  @Override
  protected AnalysisConfiguration prepareAnalysisConfig(Path projectBaseDir, List<ClientInputFile> inputFiles, Map<String, String> mergedExtraProps) {
    SonarLintLogger.get().debug("Standalone mode (project not bound)");
    return AnalysisConfiguration.builder()
      .setBaseDir(projectBaseDir)
      .addInputFiles(inputFiles.toArray(new ClientInputFile[0]))
      .putAllExtraProperties(mergedExtraProps)
      .build();
  }

  @Override
  protected AnalysisResults runAnalysis(AnalysisConfiguration analysisConfig, SonarLintIssueListener issueListener, IProgressMonitor monitor) {
    var standaloneEngine = SonarLintCorePlugin.getInstance().getDefaultSonarLintClientFacade();
    return standaloneEngine.runAnalysis(getProject(), analysisConfig, issueListener, monitor);
  }

}
