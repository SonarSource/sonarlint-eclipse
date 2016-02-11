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
package org.sonarlint.eclipse.core.internal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.osgi.framework.BundleContext;
import org.sonarlint.eclipse.core.AbstractPlugin;
import org.sonarlint.eclipse.core.internal.jobs.LogListener;
import org.sonarlint.eclipse.core.internal.jobs.SonarLintClientFacade;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProjectManager;

public class SonarLintCorePlugin extends AbstractPlugin {

  public static final String PLUGIN_ID = "org.sonarlint.eclipse.core";
  public static final String UI_PLUGIN_ID = "org.sonarlint.eclipse.ui";
  public static final String MARKER_ID = PLUGIN_ID + ".sonarlintProblem";

  private static SonarLintCorePlugin plugin;
  private static SonarLintProjectManager projectManager;

  private final List<LogListener> logListeners = new ArrayList<>();
  private SonarLintClientFacade sonarlint;
  private boolean debugEnabled;

  public SonarLintCorePlugin() {
    plugin = this;
  }

  public static SonarLintCorePlugin getDefault() {
    return plugin;
  }

  public void addLogListener(LogListener listener) {
    logListeners.add(listener);
  }

  public void removeLogListener(LogListener listener) {
    logListeners.remove(listener);
  }

  public void error(String msg) {
    for (LogListener listener : logListeners) {
      listener.error(msg);
    }
  }

  public void error(String msg, Throwable t) {
    for (LogListener listener : logListeners) {
      listener.error(msg);
      StringWriter errors = new StringWriter();
      t.printStackTrace(new PrintWriter(errors));
      listener.error(errors.toString());
    }
  }

  public void info(String msg) {
    for (LogListener listener : logListeners) {
      listener.info(msg);
    }
  }

  public void debug(String msg) {
    for (LogListener listener : logListeners) {
      listener.debug(msg);
    }
  }

  public synchronized SonarLintProjectManager getProjectManager() {
    if (projectManager == null) {
      projectManager = new SonarLintProjectManager();
    }
    return projectManager;
  }

  @Override
  public void start(BundleContext context) {
    super.start(context);
    ResourcesPlugin.getWorkspace().addResourceChangeListener(new NewProjectListener(), IResourceChangeEvent.POST_CHANGE);
  }

  @Override
  public void stop(BundleContext context) {
    if (sonarlint != null) {
      sonarlint.stop();
    }
    super.stop(context);
  }

  public boolean isDebugEnabled() {
    return debugEnabled;
  }

  public void setDebugEnabled(boolean debugEnabled) {
    this.debugEnabled = debugEnabled;
  }

  public SonarLintClientFacade getSonarLintClientFacade() {
    if (sonarlint == null) {
      sonarlint = new SonarLintClientFacade();
    }
    return sonarlint;
  }

}
