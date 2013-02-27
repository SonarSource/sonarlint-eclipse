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
package org.sonar.ide.eclipse.core.internal.resources;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.osgi.service.prefs.BackingStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.SonarKeyUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

/**
 * @author Evgeny Mandrikov
 */
public class SonarProjectManager {
  private static final Logger LOG = LoggerFactory.getLogger(SonarProjectManager.class);

  private static final String VERSION = "2";
  private static final String P_VERSION = "version";
  private static final String P_SONAR_SERVER_URL = "serverUrl";

  /**
   * @deprecated Replaced by P_PROJECT_KEY
   */
  @Deprecated
  private static final String P_PROJECT_GROUPID = "projectGroupId";

  /**
   * @deprecated Replaced by P_PROJECT_KEY
   */
  @Deprecated
  private static final String P_PROJECT_ARTIFACTID = "projectArtifactId";

  /**
   * @deprecated Replaced by P_PROJECT_KEY
   */
  @Deprecated
  private static final String P_PROJECT_BRANCH = "projectBranch";

  private static final String P_PROJECT_KEY = "projectKey";
  private static final String P_ANALYSE_LOCALLY = "analyseLocally";
  private static final String P_LAST_ANALYSIS_DATE = "lastAnalysisDate";
  private static final String P_EXTRA_ARGS = "extraArgs";

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

    SonarProject sonarProject = new SonarProject(project);
    sonarProject.setUrl(projectNode.get(P_SONAR_SERVER_URL, ""));
    sonarProject.setKey(key);
    sonarProject.setAnalysedLocally(projectNode.getBoolean(P_ANALYSE_LOCALLY, false));
    Long analysisTimestamp = projectNode.getLong(P_LAST_ANALYSIS_DATE, 0);
    if (analysisTimestamp > 0) {
      sonarProject.setLastAnalysisDate(new Date(analysisTimestamp));
    }
    String extraArgsAsString = projectNode.get(P_EXTRA_ARGS, null);
    if (extraArgsAsString != null) {
      sonarProject.setExtraArguments(new ArrayList<String>(Arrays.asList(StringUtils.split(extraArgsAsString, "\r\n"))));
    }
    return sonarProject;
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
    if (configuration.getLastAnalysisDate() != null) {
      projectNode.putLong(P_LAST_ANALYSIS_DATE, configuration.getLastAnalysisDate().getTime());
    }
    if (configuration.getExtraArguments() != null) {
      projectNode.put(P_EXTRA_ARGS, StringUtils.join(configuration.getExtraArguments(), "\r\n"));
    }
    else {
      projectNode.remove(P_EXTRA_ARGS);
    }
    try {
      projectNode.flush();
      return true;
    } catch (BackingStoreException e) {
      LOG.error("Failed to save project configuration", e);
      return false;
    }
  }
}
