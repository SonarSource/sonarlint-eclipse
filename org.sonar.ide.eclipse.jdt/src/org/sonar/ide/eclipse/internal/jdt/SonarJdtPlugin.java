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

import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.core.AbstractPlugin;

public class SonarJdtPlugin extends AbstractPlugin {

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
  public void start(BundleContext context) {
    super.start(context);
    LoggerFactory.getLogger(getClass()).debug("SonarJdtPlugin started");
  }

  @Override
  public void stop(BundleContext context) {
    super.stop(context);
    LoggerFactory.getLogger(getClass()).debug("SonarJdtPlugin stopped");
  }

}
