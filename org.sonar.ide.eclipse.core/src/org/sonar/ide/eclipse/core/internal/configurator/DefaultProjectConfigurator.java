/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2013 SonarSource
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
package org.sonar.ide.eclipse.core.internal.configurator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.sonar.ide.eclipse.core.SonarEclipseException;
import org.sonar.ide.eclipse.core.configurator.ProjectConfigurationRequest;
import org.sonar.ide.eclipse.core.configurator.ProjectConfigurator;
import org.sonar.ide.eclipse.core.internal.SonarProperties;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;

import java.util.Properties;

public class DefaultProjectConfigurator extends ProjectConfigurator {

  @Override
  public boolean canConfigure(IProject project) {
    return true;
  }

  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) {
    SonarProject remoteProject = SonarProject.getInstance(request.getProject());

    Properties properties = request.getSonarProjectProperties();
    String projectName = request.getProject().getName();
    String projectKey = remoteProject.getKey();
    String encoding;
    try {
      encoding = request.getProject().getDefaultCharset();
    } catch (CoreException e) {
      throw new SonarEclipseException("Unable to get charset from project", e);
    }

    properties.setProperty(SonarProperties.PROJECT_KEY_PROPERTY, projectKey);
    properties.setProperty(SonarProperties.PROJECT_NAME_PROPERTY, projectName);
    properties.setProperty(SonarProperties.PROJECT_VERSION_PROPERTY, "0.1-SNAPSHOT");
    properties.setProperty(SonarProperties.ENCODING_PROPERTY, encoding);
  }
}
