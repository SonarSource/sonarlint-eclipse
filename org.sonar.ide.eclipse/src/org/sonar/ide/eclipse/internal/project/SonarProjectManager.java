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

package org.sonar.ide.eclipse.internal.project;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.osgi.service.prefs.BackingStoreException;
import org.sonar.ide.api.Logs;
import org.sonar.ide.eclipse.core.ISonarConstants;
import org.sonar.ide.eclipse.internal.ui.properties.ProjectProperties;

/**
 * @author Evgeny Mandrikov
 */
public class SonarProjectManager {

  private static final String P_VERSION = "version";
  private static final String P_SONAR_SERVER_URL = "sonarServerUrl";
  private static final String P_PROJECT_GROUPID = "projectGroupId";
  private static final String P_PROJECT_ARTIFACTID = "projectArtifactId";
  private static final String P_PROJECT_BRANCH = "projectBranch";

  private static final String VERSION = "1";

  public ProjectProperties readSonarConfiguration(IProject project) {
    Logs.INFO.debug("Rading configuration for project " + project.getName());
    IScopeContext projectScope = new ProjectScope(project);
    IEclipsePreferences projectNode = projectScope.getNode(ISonarConstants.PLUGIN_ID);
    if (projectNode == null) {
      Logs.INFO.warn("Unable to read configuration");
      return new ProjectProperties(project);
    }
    String version = projectNode.get(P_VERSION, null);
    // Godin: we can perform migration here
    String artifactId = projectNode.get(P_PROJECT_ARTIFACTID, "");
    if (version == null) {
      if (StringUtils.isBlank(artifactId)) {
        artifactId = project.getName();
      }
    }

    ProjectProperties configuration = new ProjectProperties(project);
    configuration.setUrl(projectNode.get(P_SONAR_SERVER_URL, ""));
    configuration.setGroupId(projectNode.get(P_PROJECT_GROUPID, ""));
    configuration.setArtifactId(artifactId);
    configuration.setBranch(projectNode.get(P_PROJECT_BRANCH, ""));
    return configuration;
  }

  /**
   * @return false, if unable to save configuration
   */
  public boolean saveSonarConfiguration(IProject project, ProjectProperties configuration) {
    IScopeContext projectScope = new ProjectScope(project);
    IEclipsePreferences projectNode = projectScope.getNode(ISonarConstants.PLUGIN_ID);
    if (projectNode != null) {
      Logs.INFO.debug("Saving configuration for project " + project.getName());
      projectNode.put(P_VERSION, VERSION);

      projectNode.put(P_SONAR_SERVER_URL, configuration.getUrl());
      projectNode.put(P_PROJECT_GROUPID, configuration.getGroupId());
      projectNode.put(P_PROJECT_ARTIFACTID, configuration.getArtifactId());
      projectNode.put(P_PROJECT_BRANCH, configuration.getBranch());

      try {
        projectNode.flush();
        return true;
      } catch (BackingStoreException e) {
        Logs.INFO.error("Failed to save project configuration", e);
      }
    }
    return false;
  }

}
