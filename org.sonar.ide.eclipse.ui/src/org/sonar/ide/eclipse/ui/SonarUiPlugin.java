/*
 * Sonar Eclipse
 * Copyright (C) 2010-2012 SonarSource
 * dev@sonar.codehaus.org
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
package org.sonar.ide.eclipse.ui;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.sonar.ide.eclipse.core.SonarEclipseException;
import org.sonar.ide.eclipse.internal.ui.SonarImages;
import org.sonar.ide.eclipse.internal.ui.console.SonarConsole;
import org.sonar.ide.eclipse.internal.ui.jobs.RefreshViolationsJob;
import org.sonar.ide.eclipse.runner.SonarRunnerLogListener;
import org.sonar.ide.eclipse.runner.SonarRunnerPlugin;

public class SonarUiPlugin extends AbstractUIPlugin {

  // The shared instance
  private static SonarUiPlugin plugin;

  public SonarUiPlugin() {
    plugin = this; // NOSONAR
  }

  @Override
  public void start(final BundleContext context) {
    try {
      super.start(context);
    } catch (Exception e) {
      throw new SonarEclipseException("Unable to start " + context.getBundle().getSymbolicName(), e);
    }

    if (getSonarConsole() != null) {
      SonarRunnerPlugin.getDefault().addSonarLogListener((SonarRunnerLogListener) getSonarConsole());
    }

    RefreshViolationsJob.setupViolationsUpdater();
  }

  @Override
  public void stop(final BundleContext context) {
    try {
      if (getSonarConsole() != null) {
        SonarRunnerPlugin.getDefault().addSonarLogListener((SonarRunnerLogListener) getSonarConsole());
      }
    } finally {
      try {
        super.start(context);
      } catch (Exception e) {
        throw new SonarEclipseException("Unable to stop " + context.getBundle().getSymbolicName(), e);
      }
    }
  }

  /**
   * @return the shared instance
   */
  public static SonarUiPlugin getDefault() {
    return plugin;
  }

  private SonarConsole console;

  public synchronized ISonarConsole getSonarConsole() {
    // Don't try to initialize console without actual UI - it will cause headless tests failure
    if ((console == null) && PlatformUI.isWorkbenchRunning()) {
      console = new SonarConsole(SonarImages.SONAR16_IMG);
    }
    return console;
  }

}
