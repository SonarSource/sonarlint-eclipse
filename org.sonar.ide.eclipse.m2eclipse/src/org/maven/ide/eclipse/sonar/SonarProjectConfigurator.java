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

package org.maven.ide.eclipse.sonar;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;

/**
 * @author Evgeny Mandrikov
 */
public class SonarProjectConfigurator extends AbstractProjectConfigurator {
  /**
   * See <a href="http://docs.codehaus.org/display/SONAR/Advanced+parameters">advanced Maven parameters for Sonar</a>
   */
  private static final String PROJECT_BRANCH_PROPERTY = "sonar.branch";

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
    // ProjectProperties projectProperties = ProjectProperties.getInstance(project);
    //
    // projectProperties.setGroupId(mavenProject.getGroupId());
    // projectProperties.setArtifactId(mavenProject.getArtifactId());
    //
    // Properties mavenProjectProperties = mavenProject.getProperties();
    // // Don't change branch, if not set in pom.xml
    // if (mavenProjectProperties.containsKey(PROJECT_BRANCH_PROPERTY)) {
    // projectProperties.setBranch(mavenProjectProperties.getProperty(PROJECT_BRANCH_PROPERTY));
    // }
    //
    // try {
    // projectProperties.save();
    // } catch (SonarIdeException e) {
    // SonarPlugin.getDefault().displayError(IStatus.ERROR, e.getMessage(), e, true);
    // }
  }
}
