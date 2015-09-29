/*
 * SonarLint for Eclipse
 * Copyright (C) 2015 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonarlint.eclipse.core.internal.jobs;

import java.util.Properties;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.sonar.runner.api.EmbeddedRunner;
import org.sonar.runner.api.IssueListener;
import org.sonar.runner.api.LogOutput;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;

public final class SonarRunnerFacade {

  private boolean started;
  private EmbeddedRunner runner;
  private final String url;

  public SonarRunnerFacade() {
    url = DefaultScope.INSTANCE.getNode(SonarLintCorePlugin.PLUGIN_ID).get("server_url", null);
  }

  public boolean started() {
    return started;
  }

  public synchronized void startAnalysis(Properties props, IssueListener issueListener) {
    if (!started) {
      tryStart(false);
    }
    if (!started) {
      return;
    }
    if (SonarLintCorePlugin.getDefault().isDebugEnabled()) {
      props.setProperty(SonarLintProperties.VERBOSE_PROPERTY, "true");
    }
    runner.runAnalysis(props, issueListener);
  }

  public synchronized void tryUpdate() {
    stop();
    tryStart(true);
    if (!started) {
      return;
    }
    runner.syncProject(null);
  }

  private void tryStart(boolean tryUpdate) {
    Properties globalProps = new Properties();
    globalProps.setProperty(SonarLintProperties.SONAR_URL, url);
    globalProps.setProperty(SonarLintProperties.ANALYSIS_MODE, SonarLintProperties.ANALYSIS_MODE_ISSUES);
    if (SonarLintCorePlugin.getDefault().isDebugEnabled()) {
      globalProps.setProperty(SonarLintProperties.VERBOSE_PROPERTY, "true");
    }
    globalProps.setProperty(SonarLintProperties.WORK_DIR, ResourcesPlugin.getWorkspace().getRoot().getLocation().append(".sonar").toString());
    runner = EmbeddedRunner.create(new LogOutput() {

      @Override
      public void log(String msg, Level level) {
        switch (level) {
          case TRACE:
            SonarLintCorePlugin.getDefault().debug(msg + System.lineSeparator());
            break;
          case DEBUG:
            SonarLintCorePlugin.getDefault().debug(msg + System.lineSeparator());
            break;
          case INFO:
            SonarLintCorePlugin.getDefault().info(msg + System.lineSeparator());
            break;
          case WARN:
            SonarLintCorePlugin.getDefault().info(msg + System.lineSeparator());
            break;
          case ERROR:
            SonarLintCorePlugin.getDefault().error(msg + System.lineSeparator());
            break;
          default:
            SonarLintCorePlugin.getDefault().info(msg + System.lineSeparator());
        }

      }
    })
      .setApp("Eclipse", SonarLintCorePlugin.getDefault().getBundle().getVersion().toString())
      .addGlobalProperties(globalProps);
    try {
      SonarLintCorePlugin.getDefault().info("Starting SonarQube for server " + url + System.lineSeparator());
      runner.start(tryUpdate);
      String version = runner.serverVersion();
      this.started = version != null;
    } catch (Throwable e) {
      SonarLintCorePlugin.getDefault().error("Unable to start SonarQube for server " + url + System.lineSeparator(), e);
      runner = null;
      started = false;
    }
  }

  public synchronized void stop() {
    if (runner != null) {
      runner.stop();
      runner = null;
    }
    started = false;
  }

  public String getUrl() {
    return this.url;
  }

}
