/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

public class SonarJdtPlugin extends Plugin {

  public static final String PLUGIN_ID = "org.sonar.ide.eclipse.jdt"; //$NON-NLS-1$

  private static SonarJdtPlugin plugin;

  public SonarJdtPlugin() {
    plugin = this;
  }

  public static void log(Throwable e) {
    plugin.getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e));
  }

  public static void log(String message) {
    plugin.getLog().log(new Status(IStatus.INFO, PLUGIN_ID, message));
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    log("SonarJdtPlugin started");
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);
    log("SonarJdtPlugin stopped");
  }

}
