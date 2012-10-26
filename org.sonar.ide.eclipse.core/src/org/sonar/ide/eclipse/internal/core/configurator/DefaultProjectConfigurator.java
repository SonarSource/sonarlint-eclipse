/*
 * Sonar Eclipse
 * Copyright (C) 2010-2012 SonarSource
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
package org.sonar.ide.eclipse.internal.core.configurator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.sonar.api.CoreProperties;
import org.sonar.ide.eclipse.core.configurator.ProjectConfigurationRequest;
import org.sonar.ide.eclipse.core.configurator.ProjectConfigurator;
import org.sonar.ide.eclipse.internal.core.resources.ProjectProperties;

import java.util.Properties;

public class DefaultProjectConfigurator extends ProjectConfigurator {
  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) {
    ProjectProperties remoteProject = ProjectProperties.getInstance(request.getProject());

    Properties properties = request.getSonarProject().getProperties();
    String projectName = request.getProject().getName();
    String projectKey = remoteProject.getKey();

    properties.setProperty(CoreProperties.PROJECT_KEY_PROPERTY, projectKey);
    properties.setProperty(CoreProperties.PROJECT_NAME_PROPERTY, projectName);
    properties.setProperty(CoreProperties.PROJECT_VERSION_PROPERTY, "0.1-SNAPSHOT");
  }
}
