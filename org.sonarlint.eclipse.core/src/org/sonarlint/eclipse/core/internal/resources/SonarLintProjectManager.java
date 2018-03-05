/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2018 SonarSource SA
 * sonarlint@sonarsource.com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.core.internal.resources;

import java.util.List;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.osgi.service.prefs.BackingStoreException;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.utils.PreferencesUtils;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;

public class SonarLintProjectManager {

  private static final String P_EXTRA_PROPS = "extraProperties";
  private static final String P_FILE_EXCLUSIONS = "fileExclusions";
  private static final String P_SERVER_ID = "serverId";
  private static final String P_PROJECT_KEY = "projectKey";
  private static final String P_MODULE_KEY = "moduleKey";
  private static final String P_AUTO_ENABLED_KEY = "autoEnabled";

  public SonarLintProjectConfiguration readSonarLintConfiguration(IScopeContext projectScope) {
    IEclipsePreferences projectNode = projectScope.getNode(SonarLintCorePlugin.PLUGIN_ID);
    SonarLintProjectConfiguration sonarProject = new SonarLintProjectConfiguration(projectScope);
    if (projectNode == null) {
      return sonarProject;
    }

    String extraArgsAsString = projectNode.get(P_EXTRA_PROPS, null);
    List<SonarLintProperty> sonarProperties = PreferencesUtils.deserializeExtraProperties(extraArgsAsString);
    String fileExclusionsAsString = projectNode.get(P_FILE_EXCLUSIONS, null);
    List<ExclusionItem> fileExclusions = PreferencesUtils.deserializeFileExclusions(fileExclusionsAsString);

    sonarProject.setExtraProperties(sonarProperties);
    sonarProject.setFileExclusions(fileExclusions);
    sonarProject.setProjectKey(projectNode.get(P_PROJECT_KEY, ""));
    sonarProject.setModuleKey(projectNode.get(P_MODULE_KEY, ""));
    sonarProject.setServerId(projectNode.get(P_SERVER_ID, ""));
    sonarProject.setAutoEnabled(projectNode.getBoolean(P_AUTO_ENABLED_KEY, true));
    return sonarProject;
  }

  /**
   * @return false, if unable to save configuration
   */
  public boolean saveSonarLintConfiguration(IScopeContext projectScope, SonarLintProjectConfiguration configuration) {
    IEclipsePreferences projectNode = projectScope.getNode(SonarLintCorePlugin.PLUGIN_ID);
    if (projectNode == null) {
      return false;
    }

    if (configuration.getExtraProperties() != null) {
      String props = PreferencesUtils.serializeExtraProperties(configuration.getExtraProperties());
      projectNode.put(P_EXTRA_PROPS, props);
    } else {
      projectNode.remove(P_EXTRA_PROPS);
    }

    if (configuration.getFileExclusions() != null) {
      String props = PreferencesUtils.serializeFileExclusions(configuration.getFileExclusions());
      projectNode.put(P_FILE_EXCLUSIONS, props);
    } else {
      projectNode.remove(P_FILE_EXCLUSIONS);
    }

    if (StringUtils.isNotBlank(configuration.getProjectKey())) {
      projectNode.put(P_PROJECT_KEY, configuration.getProjectKey());
    } else {
      projectNode.remove(P_PROJECT_KEY);
    }

    if (StringUtils.isNotBlank(configuration.getModuleKey())) {
      projectNode.put(P_MODULE_KEY, configuration.getModuleKey());
    } else {
      projectNode.remove(P_MODULE_KEY);
    }

    if (StringUtils.isNotBlank(configuration.getServerId())) {
      projectNode.put(P_SERVER_ID, configuration.getServerId());
    } else {
      projectNode.remove(P_SERVER_ID);
    }

    projectNode.putBoolean(P_AUTO_ENABLED_KEY, configuration.isAutoEnabled());
    try {
      projectNode.flush();
      return true;
    } catch (BackingStoreException e) {
      SonarLintLogger.get().error("Failed to save project configuration", e);
      return false;
    }
  }

}
