/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.core.ISonarProject;
import org.sonar.ide.eclipse.core.SonarCorePlugin;
import org.sonar.ide.eclipse.internal.core.resources.ProjectProperties;
import org.sonar.ide.eclipse.internal.ui.FavouriteMetricsManager;
import org.sonar.ide.eclipse.internal.ui.SonarImages;
import org.sonar.ide.eclipse.internal.ui.console.SonarConsole;
import org.sonar.ide.eclipse.internal.ui.jobs.RefreshViolationsJob;
import org.sonar.ide.eclipse.internal.ui.preferences.SonarUiPreferenceInitializer;

public class SonarUiPlugin extends AbstractUIPlugin {

  // The shared instance
  private static SonarUiPlugin plugin;

  private FavouriteMetricsManager favouriteMetricsManager = new FavouriteMetricsManager();

  public SonarUiPlugin() {
    plugin = this; // NOSONAR
  }

  public static FavouriteMetricsManager getFavouriteMetricsManager() {
    return getDefault().favouriteMetricsManager;
  }

  @Override
  public void start(final BundleContext context) throws Exception {
    super.start(context);

    RefreshViolationsJob.setupViolationsUpdater();

    getFavouriteMetricsManager().set(SonarUiPreferenceInitializer.getFavouriteMetrics());
  }

  @Override
  public void stop(final BundleContext context) throws Exception {
    try {
      SonarUiPreferenceInitializer.setFavouriteMetrics(getFavouriteMetricsManager().get());
    } finally {
      super.stop(context);
    }
  }

  /**
   * @return the shared instance
   */
  public static SonarUiPlugin getDefault() {
    return plugin;
  }

  public static boolean hasSonarNature(IProject project) {
    try {
      return project.hasNature(SonarCorePlugin.NATURE_ID);
    } catch (CoreException e) {
      LoggerFactory.getLogger(SonarUiPlugin.class).error(e.getMessage(), e);
      return false;
    }
  }

  public static boolean hasJavaNature(IProject project) {
    try {
      return project.hasNature("org.eclipse.jdt.core.javanature");
    } catch (CoreException e) {
      LoggerFactory.getLogger(SonarUiPlugin.class).error(e.getMessage(), e);
      return false;
    }
  }

  public static ISonarProject getSonarProject(IProject project) {
    return ProjectProperties.getInstance(project);
  }

  private SonarConsole console;

  public synchronized ISonarConsole getSonarConsole() {
    if (console == null) {
      console = new SonarConsole(SonarImages.SONAR16_IMG);
    }
    return console;
  }

}
