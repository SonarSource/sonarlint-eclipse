/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2020 SonarSource SA
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.IProgressMonitor;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.StandaloneEngineFacade;
import org.sonarlint.eclipse.core.internal.preferences.RuleConfig;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.common.RuleKey;
import org.sonarsource.sonarlint.core.client.api.common.analysis.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneAnalysisConfiguration;

import static java.util.stream.Collectors.toList;

public class AnalyzeStandaloneProjectJob extends AbstractAnalyzeProjectJob<StandaloneAnalysisConfiguration> {

  private final Collection<RuleConfig> rulesConfig;

  public AnalyzeStandaloneProjectJob(AnalyzeProjectRequest request) {
    super(request);
    rulesConfig = SonarLintGlobalConfiguration.readRulesConfig();
  }

  @Override
  protected StandaloneAnalysisConfiguration prepareAnalysisConfig(Path projectBaseDir, List<ClientInputFile> inputFiles, Map<String, String> mergedExtraProps) {
    SonarLintLogger.get().debug("Standalone mode (project not bound)");
    return StandaloneAnalysisConfiguration.builder()
      .setBaseDir(projectBaseDir)
      .addInputFiles(inputFiles)
      .putAllExtraProperties(mergedExtraProps)
      .addExcludedRules(getExcludedRules())
      .addIncludedRules(getIncludedRules())
      .addRuleParameters(getRuleParameters())
      .build();
  }

  @Override
  protected AnalysisResults runAnalysis(StandaloneAnalysisConfiguration analysisConfig, SonarLintIssueListener issueListener, IProgressMonitor monitor) {
    StandaloneEngineFacade standaloneEngine = SonarLintCorePlugin.getInstance().getDefaultSonarLintClientFacade();
    return standaloneEngine.runAnalysis(analysisConfig, issueListener, monitor);
  }

  private Collection<RuleKey> getExcludedRules() {
    return rulesConfig.stream()
      .filter(r -> !r.isActive())
      .map(r -> RuleKey.parse(r.getKey()))
      .collect(toList());
  }

  private Collection<RuleKey> getIncludedRules() {
    return rulesConfig.stream()
      .filter(RuleConfig::isActive)
      .map(r -> RuleKey.parse(r.getKey()))
      .collect(toList());
  }

  private Map<RuleKey, Map<String, String>> getRuleParameters() {
    return rulesConfig.stream()
      .filter(RuleConfig::isActive)
      .filter(r -> !r.getParams().isEmpty())
      .collect(Collectors.toMap(r -> RuleKey.parse(r.getKey()), RuleConfig::getParams));
  }
}
