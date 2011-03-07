/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2011 SonarSource
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
package org.sonar.ide.eclipse.internal.core.configurator;

import java.util.Properties;

import org.eclipse.core.runtime.IProgressMonitor;
import org.sonar.api.CoreProperties;
import org.sonar.ide.eclipse.core.configurator.AbstractProjectConfigurator;
import org.sonar.ide.eclipse.core.configurator.ProjectConfigurationRequest;

public class DefaultProjectConfigurator extends AbstractProjectConfigurator {
  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) {
    // TODO remove hard-coded values
    Properties properties = request.getSonarProject().getProperties();
    String projectName = request.getProject().getName();
    String projectKey = "org.example:" + request.getProject().getName();
    properties.setProperty(CoreProperties.PROJECT_KEY_PROPERTY, projectKey);
    properties.setProperty(CoreProperties.PROJECT_NAME_PROPERTY, projectName);
    properties.setProperty(CoreProperties.PROJECT_VERSION_PROPERTY, "0.1-SNAPSHOT");
  }
}
