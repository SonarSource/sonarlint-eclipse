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
import org.eclipse.core.resources.ResourcesPlugin;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarsource.sonarlint.core.AnalysisConfiguration;
import org.sonarsource.sonarlint.core.IssueListener;
import org.sonarsource.sonarlint.core.LogOutput;
import org.sonarsource.sonarlint.core.SonarLintClient;

public final class SonarLintClientFacade {

  private final class DefaultLogOutput implements LogOutput {
    @Override
    public void log(String msg, Level level) {
      switch (level) {
        case TRACE:
        case DEBUG:
          SonarLintCorePlugin.getDefault().debug(msg);
          break;
        case INFO:
        case WARN:
          SonarLintCorePlugin.getDefault().info(msg);
          break;
        case ERROR:
          SonarLintCorePlugin.getDefault().error(msg);
          break;
        default:
          SonarLintCorePlugin.getDefault().info(msg);
      }

    }
  }

  private boolean started;
  private SonarLintClient client;

  public boolean started() {
    return started;
  }

  public synchronized void startAnalysis(AnalysisConfiguration config, IssueListener issueListener) {
    if (!started) {
      tryStart();
    }
    if (!started) {
      return;
    }
    client.analyze(config, issueListener);
  }

  private void tryStart() {
    client = SonarLintClient.builder()
      .addPlugins(Collections.list(SonarLintCorePlugin.getDefault().getBundle().findEntries("/plugins", "*.jar", false)).toArray(new URL[0]))
      .setVerbose(SonarLintCorePlugin.getDefault().isDebugEnabled())
      .setWorkDir(ResourcesPlugin.getWorkspace().getRoot().getLocation().append(".sonar").toFile().toPath())
      .setLogOutput(new DefaultLogOutput())
      .build();
    try {
      SonarLintCorePlugin.getDefault().info("Starting SonarLint");
      client.start();
      this.started = true;
    } catch (Throwable e) {
      SonarLintCorePlugin.getDefault().error("Unable to start SonarLint", e);
      client = null;
      started = false;
    }
  }

  public String getHtmlRuleDescription(String ruleKey) {
    if (!started) {
      tryStart();
    }
    if (!started) {
      return "Unavailable";
    }
    return client.getHtmlRuleDescription(ruleKey);
  }

  public synchronized void stop() {
    if (client != null) {
      client.stop();
      client = null;
    }
    started = false;
  }

}
