/*
 * SonarLint for Eclipse
 * Copyright (C) 2015 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonarlint.eclipse.core.configurator;

import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

public class ProjectConfigurationRequest {

  private final IProject project;
  private final Properties sonarProjectProperties;
  private final Collection<IFile> filesToAnalyze;

  public ProjectConfigurationRequest(IProject eclipseProject, Collection<IFile> filesToAnalyze, Properties sonarProjectProperties) {
    this.project = eclipseProject;
    this.sonarProjectProperties = sonarProjectProperties;
    this.filesToAnalyze = filesToAnalyze != null ? Collections.unmodifiableCollection(filesToAnalyze) : null;
  }

  public IProject getProject() {
    return project;
  }

  /**
   * Analysis properties. Can be modified by the configurator.
   */
  public Properties getSonarProjectProperties() {
    return sonarProjectProperties;
  }

  /**
   * Read-only list of files to analyze "on-the-fly" or <code>null</code> if full project analysis.
   */
  public Collection<IFile> getFilesToAnalyze() {
    return filesToAnalyze;
  }

}
