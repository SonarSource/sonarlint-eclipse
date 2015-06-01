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
package org.sonar.ide.eclipse.core.configurator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.wsclient.SonarVersionTester;

import java.util.Properties;

public class ProjectConfigurationRequest {

  private final IProject project;
  private final IEclipsePreferences projectNode;
  private final Properties sonarProjectProperties;
  private final String serverVersion;

  public ProjectConfigurationRequest(IProject eclipseProject, Properties sonarProjectProperties, String serverVersion) {
    this.project = eclipseProject;
    IScopeContext projectScope = new ProjectScope(project);
	projectNode = projectScope.getNode(SonarCorePlugin.PLUGIN_ID);
    this.sonarProjectProperties = sonarProjectProperties;
    this.serverVersion = serverVersion;
  }

  public IProject getProject() {
    return project;
  }
  
  public IEclipsePreferences getProjectNode() {
    return projectNode;
  }

  public Properties getSonarProjectProperties() {
    return sonarProjectProperties;
  }

  public boolean isServerVersionGreaterOrEquals(String version) {
    return SonarVersionTester.isServerVersionSupported(version, serverVersion);
  }

}
