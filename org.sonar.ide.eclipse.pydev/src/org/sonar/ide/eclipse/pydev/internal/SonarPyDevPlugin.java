/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2014 SonarSource
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
package org.sonar.ide.eclipse.pydev.internal;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.python.pydev.plugin.nature.PythonNature;
import org.sonar.ide.eclipse.core.AbstractPlugin;
import org.sonar.ide.eclipse.core.SonarEclipseException;

public class SonarPyDevPlugin extends AbstractPlugin {

  public static final String PLUGIN_ID = "org.sonar.ide.eclipse.pydev"; //$NON-NLS-1$

  private static SonarPyDevPlugin plugin;

  public SonarPyDevPlugin() {
    plugin = this;
  }

  /**
   * @return the shared instance
   */
  public static SonarPyDevPlugin getDefault() {
    return plugin;
  }

  static String getRelativePath(IPath rootPath, IPath path) {
    return path.makeRelativeTo(rootPath).toString();
  }

  static String[] getSourceFolders(PythonNature pyProject) {
    String projectSourcePath;
    try {
      projectSourcePath = pyProject.getPythonPathNature().getProjectSourcePath(true);
    } catch (CoreException e) {
      throw new SonarEclipseException("Error when getting Python source folder", e);
    }
    return StringUtils.split(projectSourcePath, '|');
  }

}
