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

  public AnalyzeStandaloneProjectJob(AnalyzeProjectRequest request) {
    super(request);
  }

  @Override
  protected StandaloneAnalysisConfiguration prepareAnalysisConfig(Path projectBaseDir, List<ClientInputFile> inputFiles, Map<String, String> mergedExtraProps) {
    SonarLintLogger.get().debug("Standalone mode (project not bound)");
    Collection<RuleConfig> rulesConfig = SonarLintGlobalConfiguration.readRulesConfig();
    return StandaloneAnalysisConfiguration.builder()
      .setBaseDir(projectBaseDir)
      .addInputFiles(inputFiles)
      .putAllExtraProperties(mergedExtraProps)
      .addExcludedRules(getExcludedRules(rulesConfig))
      .addIncludedRules(getIncludedRules(rulesConfig))
      .addRuleParameters(getRuleParameters(rulesConfig))
      .build();
  }

  @Override
  protected AnalysisResults runAnalysis(StandaloneAnalysisConfiguration analysisConfig, SonarLintIssueListener issueListener, IProgressMonitor monitor) {
    StandaloneEngineFacade standaloneEngine = SonarLintCorePlugin.getInstance().getDefaultSonarLintClientFacade();
    return standaloneEngine.runAnalysis(analysisConfig, issueListener, monitor);
  }

  private static Collection<RuleKey> getExcludedRules(Collection<RuleConfig> rulesConfig) {
    return rulesConfig.stream()
      .filter(r -> !r.isActive())
      .map(r -> RuleKey.parse(r.getKey()))
      .collect(toList());
  }

  private static Collection<RuleKey> getIncludedRules(Collection<RuleConfig> rulesConfig) {
    return rulesConfig.stream()
      .filter(RuleConfig::isActive)
      .map(r -> RuleKey.parse(r.getKey()))
      .collect(toList());
  }

  private static Map<RuleKey, Map<String, String>> getRuleParameters(Collection<RuleConfig> rulesConfig) {
    return rulesConfig.stream()
      .filter(RuleConfig::isActive)
      .filter(r -> !r.getParams().isEmpty())
      .collect(Collectors.toMap(r -> RuleKey.parse(r.getKey()), RuleConfig::getParams));
  }
}
