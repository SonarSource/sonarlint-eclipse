/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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
import java.util.Collections;
import java.util.Enumeration;
import org.eclipse.core.resources.ResourcesPlugin;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarsource.sonarlint.core.SonarLintEngineImpl;
import org.sonarsource.sonarlint.core.client.api.GlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.RuleDetails;
import org.sonarsource.sonarlint.core.client.api.SonarLintEngine;
import org.sonarsource.sonarlint.core.client.api.analysis.AnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.analysis.IssueListener;
import org.sonarsource.sonarlint.core.client.api.connected.ServerConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ValidationResult;

public class StandaloneSonarLintClientFacade {

  private SonarLintEngine client;

  private synchronized SonarLintEngine getClient() {
    if (client == null) {
      SonarLintCorePlugin.getDefault().info("Starting standalone SonarLint engine");
      Enumeration<URL> pluginEntries = SonarLintCorePlugin.getDefault().getBundle().findEntries("/plugins", "*.jar", false);
      GlobalConfiguration globalConfig = GlobalConfiguration.builder()
        .addPlugins(pluginEntries != null ? Collections.list(pluginEntries).toArray(new URL[0]) : new URL[0])
        .setVerbose(SonarLintCorePlugin.getDefault().isDebugEnabled())
        .setWorkDir(ResourcesPlugin.getWorkspace().getRoot().getLocation().append(".sonarlint").append("default").toFile().toPath())
        .setLogOutput(new SonarLintLogOutput())
        .build();
      try {
        client = new SonarLintEngineImpl(globalConfig);
      } catch (Throwable e) {
        SonarLintCorePlugin.getDefault().error("Unable to start SonarLint engine", e);
        client = null;
      }
    }
    return client;
  }

  public void startAnalysis(AnalysisConfiguration config, IssueListener issueListener) {
    getClient().analyze(config, issueListener);
  }

  public String getHtmlRuleDescription(String ruleKey) {
    RuleDetails ruleDetails = getClient().getRuleDetails(ruleKey);
    return ruleDetails != null ? ruleDetails.getHtmlDescription() : "Not found";
  }

  public synchronized void stop() {
    if (client != null) {
      client.stop();
      client = null;
    }
  }

  public ValidationResult testConnection(ServerConfiguration config) {
    return getClient().validateCredentials(config);
  }

  public void setVerbose(boolean verbose) {
    if (client != null) {
      client.setVerbose(verbose);
    }
  }

}
