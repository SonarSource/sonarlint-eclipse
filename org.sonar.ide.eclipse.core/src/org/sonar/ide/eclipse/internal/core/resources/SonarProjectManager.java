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
package org.sonar.ide.eclipse.internal.core.resources;

import org.sonar.ide.eclipse.internal.core.SonarKeyUtils;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.osgi.service.prefs.BackingStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.core.SonarCorePlugin;

/**
 * @author Evgeny Mandrikov
 */
public class SonarProjectManager {
  private static final Logger LOG = LoggerFactory.getLogger(SonarProjectManager.class);

  private static final String VERSION = "2";
  private static final String P_VERSION = "version";
  private static final String P_SONAR_SERVER_URL = "serverUrl";
  @Deprecated
  private static final String P_PROJECT_GROUPID = "projectGroupId";
  @Deprecated
  private static final String P_PROJECT_ARTIFACTID = "projectArtifactId";
  @Deprecated
  private static final String P_PROJECT_BRANCH = "projectBranch";
  private static final String P_PROJECT_KEY = "projectKey";
  private static final String P_ANALYSE_LOCALLY = "analyseLocally";

  public SonarProject readSonarConfiguration(IProject project) {
    LOG.debug("Reading configuration for project " + project.getName());

    IScopeContext projectScope = new ProjectScope(project);
    IEclipsePreferences projectNode = projectScope.getNode(SonarCorePlugin.PLUGIN_ID);
    if (projectNode == null) {
      LOG.error("Unable to read configuration for project " + project.getName());
      return new SonarProject(project);
    }

    String version = projectNode.get(P_VERSION, null);
    // Godin: we can perform migration here
    final String key;

    if (version == null) {
      String artifactId = projectNode.get(P_PROJECT_ARTIFACTID, "");
      if (StringUtils.isBlank(artifactId)) {
        artifactId = project.getName();
      }
      String groupId = projectNode.get(P_PROJECT_GROUPID, "");
      String branch = projectNode.get(P_PROJECT_BRANCH, "");
      key = SonarKeyUtils.projectKey(groupId, artifactId, branch);
    } else if ("1".equals(version)) {
      String artifactId = projectNode.get(P_PROJECT_ARTIFACTID, "");
      String groupId = projectNode.get(P_PROJECT_GROUPID, "");
      String branch = projectNode.get(P_PROJECT_BRANCH, "");
      key = SonarKeyUtils.projectKey(groupId, artifactId, branch);
    }
    else {
      key = projectNode.get(P_PROJECT_KEY, "");
    }

    SonarProject properties = new SonarProject(project);
    properties.setUrl(projectNode.get(P_SONAR_SERVER_URL, ""));
    properties.setKey(key);
    properties.setAnalysedLocally(projectNode.getBoolean(P_ANALYSE_LOCALLY, false));
    return properties;
  }

  /**
   * @return false, if unable to save configuration
   */
  public boolean saveSonarConfiguration(IProject project, SonarProject configuration) {
    IScopeContext projectScope = new ProjectScope(project);
    IEclipsePreferences projectNode = projectScope.getNode(SonarCorePlugin.PLUGIN_ID);
    if (projectNode == null) {
      return false;
    }

    LOG.debug("Saving configuration for project " + project.getName());
    projectNode.put(P_VERSION, VERSION);

    projectNode.put(P_SONAR_SERVER_URL, configuration.getUrl());
    projectNode.put(P_PROJECT_KEY, configuration.getKey());
    projectNode.putBoolean(P_ANALYSE_LOCALLY, configuration.isAnalysedLocally());

    try {
      projectNode.flush();
      return true;
    } catch (BackingStoreException e) {
      LOG.error("Failed to save project configuration", e);
      return false;
    }
  }
}
