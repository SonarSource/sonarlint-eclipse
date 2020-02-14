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

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarsource.sonarlint.core.StandaloneSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.client.api.common.RuleDetails;
import org.sonarsource.sonarlint.core.client.api.common.analysis.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.analysis.IssueListener;
import org.sonarsource.sonarlint.core.client.api.connected.Language;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneSonarLintEngine;

public class StandaloneSonarLintEngineFacade {

  private StandaloneSonarLintEngine client;

  @CheckForNull
  private synchronized StandaloneSonarLintEngine getClient() {
    if (client == null) {
      SonarLintLogger.get().info("Starting standalone SonarLint engine " + SonarLintUtils.getPluginVersion() + "...");
      Enumeration<URL> pluginEntriesEnum = SonarLintCorePlugin.getInstance().getBundle().findEntries("/plugins", "*.jar", false);
      if (pluginEntriesEnum != null) {
        List<URL> pluginEntries = Collections.list(pluginEntriesEnum);
        SonarLintLogger.get().debug("Loading embedded analyzers...");
        pluginEntries.stream().forEach(e -> SonarLintLogger.get().debug("  - " + e.getFile()));
        StandaloneGlobalConfiguration globalConfig = StandaloneGlobalConfiguration.builder()
          .addPlugins(pluginEntries.toArray(new URL[0]))
          .setWorkDir(ResourcesPlugin.getWorkspace().getRoot().getLocation().append(".sonarlint").append("default").toFile().toPath())
          .setLogOutput(new SonarLintAnalyzerLogOutput())
          .build();
        try {
          client = new StandaloneSonarLintEngineImpl(globalConfig);
        } catch (Throwable e) {
          SonarLintLogger.get().error("Unable to start standalone SonarLint engine", e);
          client = null;
        }
      } else {
        throw new IllegalStateException("Unable to find any embedded plugin");
      }
    }
    return client;
  }

  @CheckForNull
  public AnalysisResults runAnalysis(StandaloneAnalysisConfiguration config, IssueListener issueListener, IProgressMonitor monitor) {
    StandaloneSonarLintEngine engine = getClient();
    if (engine != null) {
      return engine.analyze(config, issueListener, null, new WrappedProgressMonitor(monitor, "Analysis"));
    }
    return null;
  }

  @CheckForNull
  public RuleDetails getRuleDescription(String ruleKey) {
    StandaloneSonarLintEngine engine = getClient();
    if (engine != null) {
      return engine.getRuleDetails(ruleKey).orElse(null);
    }
    return null;
  }

  public Collection<RuleDetails> getAllRuleDetails() {
    StandaloneSonarLintEngine engine = getClient();
    if (engine != null) {
      return engine.getAllRuleDetails()
        .stream()
        .filter(r -> isNotTypeScript(r.getLanguageKey()))
        .collect(Collectors.toSet());
    }
    return Collections.emptyList();
  }

  public Map<String, String> getAllLanguagesNameByKey() {
    StandaloneSonarLintEngine engine = getClient();
    if (engine != null) {
      return engine.getAllLanguagesNameByKey()
        .entrySet()
        .stream()
        .filter(e -> isNotTypeScript(e.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    return Collections.emptyMap();
  }

  private static boolean isNotTypeScript(String key) {
    return !Language.TS.getLanguageKey().equals(key);
  }

  public synchronized void stop() {
    if (client != null) {
      client.stop();
      client = null;
    }
  }

}
