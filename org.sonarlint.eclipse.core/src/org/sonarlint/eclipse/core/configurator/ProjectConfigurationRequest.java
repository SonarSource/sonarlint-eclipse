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
package org.sonarlint.eclipse.core.configurator;

import java.util.Collection;
import java.util.Map;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

public class ProjectConfigurationRequest {

  private final IProject project;
  private final Map<String, String> sonarProjectProperties;
  private final Collection<IFile> filesToAnalyze;

  public ProjectConfigurationRequest(IProject eclipseProject, Collection<IFile> filesToAnalyze, Map<String, String> sonarProjectProperties) {
    this.project = eclipseProject;
    this.sonarProjectProperties = sonarProjectProperties;
    this.filesToAnalyze = filesToAnalyze;
  }

  public IProject getProject() {
    return project;
  }

  /**
   * Analysis properties. Can be modified by the configurator.
   */
  public Map<String, String> getSonarProjectProperties() {
    return sonarProjectProperties;
  }

  /**
   * List of files to analyze. The project configurator can decide to exclude some of them, or replace virtual files with real files.
   */
  public Collection<IFile> getFilesToAnalyze() {
    return filesToAnalyze;
  }

}
