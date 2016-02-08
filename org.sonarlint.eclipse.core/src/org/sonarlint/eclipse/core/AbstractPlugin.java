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
package org.sonarlint.eclipse.core;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

public class AbstractPlugin extends Plugin {

  /**
   * @throws SonarEclipseException if this plug-in did not start up properly
   */
  @Override
  public void start(BundleContext context) {
    try {
      super.start(context);
    } catch (Exception e) {
      throw new SonarEclipseException("Unable to start " + context.getBundle().getSymbolicName(), e);
    }
  }

  /**
   * @throws SonarEclipseException if this plug-in did not start up properly
   */
  @Override
  public void stop(BundleContext context) {
    try {
      super.stop(context);
    } catch (Exception e) {
      throw new SonarEclipseException("Unable to stop " + context.getBundle().getSymbolicName(), e);
    }
  }

}
