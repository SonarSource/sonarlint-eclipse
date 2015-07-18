/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.ide.eclipse.cdt.internal;

import java.util.Properties;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IIncludeReference;
import org.eclipse.cdt.core.model.ISourceRoot;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.sonar.ide.eclipse.core.configurator.ProjectConfigurationRequest;
import org.sonar.ide.eclipse.core.configurator.ProjectConfigurator;
import org.sonar.ide.eclipse.core.configurator.SonarConfiguratorProperties;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;

public class CProjectConfigurator extends ProjectConfigurator {

  @Override
  public boolean canConfigure(IProject project) {
    return CoreModel.hasCNature(project);
  }

  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) {
    IProject project = request.getProject();
    ICProject cProject = CoreModel.getDefault().create(project);
    configureCProject(cProject, request.getSonarProjectProperties());
  }

  private void configureCProject(ICProject cProject, Properties sonarProjectProperties) {
    try {
      ISourceRoot[] sourceRoots = cProject.getAllSourceRoots();
      for (ISourceRoot sourceRoot : sourceRoots) {
        appendProperty(sonarProjectProperties, SonarConfiguratorProperties.SOURCE_DIRS_PROPERTY, getAbsolutePath(sourceRoot.getPath()));
      }
      IIncludeReference[] includes = cProject.getIncludeReferences();
      for (IIncludeReference include : includes) {
        appendProperty(sonarProjectProperties, "sonar.cpp.library.directories", getAbsolutePath(include.getPath()));
      }
    } catch (CModelException e) {
      SonarCorePlugin.getDefault().error(e.getMessage(), e);
    }
  }
}
