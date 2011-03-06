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
package org.sonar.ide.eclipse.internal.jdt;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;
import org.sonar.ide.eclipse.internal.jdt.profiles.RetrieveSonarProfileJob;
import org.sonar.ide.eclipse.ui.SonarUiPlugin;
import org.slf4j.LoggerFactory;

public class SonarJdtPlugin extends Plugin {

  private static final String PREF_SYNCHRONISE_PROFILE = "synchroniseProfile";
  
  public static final String PLUGIN_ID = "org.sonar.ide.eclipse.jdt"; //$NON-NLS-1$

  private static SonarJdtPlugin plugin;

  public SonarJdtPlugin() {
    plugin = this; // NOSONAR
  }

  /**
   * @return the shared instance
   */
  public static SonarJdtPlugin getDefault() {
    return plugin;
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    if(SonarUiPlugin.getDefault().getPreferenceStore().getBoolean(PREF_SYNCHRONISE_PROFILE)) {
        new RetrieveSonarProfileJob().schedule();
    }
    LoggerFactory.getLogger(getClass()).debug("SonarJdtPlugin started");
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);
    LoggerFactory.getLogger(getClass()).debug("SonarJdtPlugin stopped");
  }

}
