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
package org.sonarlint.eclipse.cdt.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.cdt.core.model.CoreModel;
import org.sonarlint.eclipse.core.AbstractPlugin;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;

public class SonarCdtPlugin extends AbstractPlugin {

  public static final String PLUGIN_ID = "org.sonarlint.eclipse.cdt"; //$NON-NLS-1$

  private static SonarCdtPlugin plugin;

  public SonarCdtPlugin() {
    plugin = this;
  }

  /**
   * @return the shared instance
   */
  public static SonarCdtPlugin getDefault() {
    return plugin;
  }

  public static boolean hasCNature(IProject project) {
    try {
      return project.hasNature(CProjectNature.C_NATURE_ID) || project.hasNature(CCProjectNature.CC_NATURE_ID);
    } catch (CoreException e) {
      SonarLintCorePlugin.getDefault().error(e.getMessage(), e);
      return false;
    }
  }

}
