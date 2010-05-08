/*
 * Copyright (C) 2010 Evgeny Mandrikov
 *
 * Sonar-IDE is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar-IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar-IDE; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.maven.ide.eclipse.sonar;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;
import org.sonar.ide.api.SonarIdeException;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.properties.ProjectProperties;

/**
 * @author Evgeny Mandrikov
 */
public class SonarProjectConfigurator extends AbstractProjectConfigurator {
  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    configureProject(request.getProject(), request.getMavenProject());
  }

  @Override
  protected void mavenProjectChanged(MavenProjectChangedEvent event, IProgressMonitor monitor) throws CoreException {
    IMavenProjectFacade facade = event.getMavenProject();
    configureProject(facade.getProject(), facade.getMavenProject());
  }

  private void configureProject(IProject project, MavenProject mavenProject) {
    String groupId = mavenProject.getGroupId();
    String artifactId = mavenProject.getArtifactId();
    ProjectProperties projectProperties = ProjectProperties.getInstance(project);
    projectProperties.setGroupId(groupId);
    projectProperties.setArtifactId(artifactId);
    try {
      projectProperties.save();
    } catch (SonarIdeException e) {
      SonarPlugin.getDefault().displayError(IStatus.ERROR, e.getMessage(), e, true);
    }
  }
}
