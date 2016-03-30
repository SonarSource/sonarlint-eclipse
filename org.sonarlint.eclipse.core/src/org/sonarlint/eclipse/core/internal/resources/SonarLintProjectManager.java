/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.osgi.service.prefs.BackingStoreException;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;

public class SonarLintProjectManager {

  private static final String P_EXTRA_PROPS = "extraProperties";
  private static final String P_SERVER_ID = "serverId";
  private static final String P_MODULE_KEY = "moduleKey";
  private static final String P_AUTO_ENABLED_KEY = "autoEnabled";

  public SonarLintProject readSonarLintConfiguration(IProject project) {
    IScopeContext projectScope = new ProjectScope(project);
    IEclipsePreferences projectNode = projectScope.getNode(SonarLintCorePlugin.PLUGIN_ID);
    if (projectNode == null) {
      return new SonarLintProject(project);
    }

    SonarLintProject sonarProject = new SonarLintProject(project);
    String extraArgsAsString = projectNode.get(P_EXTRA_PROPS, null);
    List<SonarLintProperty> sonarProperties = new ArrayList<>();
    if (extraArgsAsString != null) {
      try {
        String[] props = StringUtils.split(extraArgsAsString, "\r\n");
        for (String keyValuePair : props) {
          String[] keyValue = keyValuePair.split("=");
          sonarProperties.add(new SonarLintProperty(keyValue[0], keyValue.length > 1 ? keyValue[1] : ""));
        }
      } catch (Exception e) {
        SonarLintCorePlugin.getDefault().error("Error while loading SonarLint properties", e);
      }
    }
    sonarProject.setExtraProperties(sonarProperties);
    sonarProject.setModuleKey(projectNode.get(P_MODULE_KEY, ""));
    sonarProject.setServerId(projectNode.get(P_SERVER_ID, ""));
    sonarProject.setAutoEnabled(projectNode.getBoolean(P_AUTO_ENABLED_KEY, true));
    return sonarProject;
  }

  /**
   * @return false, if unable to save configuration
   */
  public boolean saveSonarLintConfiguration(IProject project, SonarLintProject configuration) {
    IScopeContext projectScope = new ProjectScope(project);
    IEclipsePreferences projectNode = projectScope.getNode(SonarLintCorePlugin.PLUGIN_ID);
    if (projectNode == null) {
      return false;
    }

    if (configuration.getExtraProperties() != null) {
      List<String> keyValuePairs = new ArrayList<>(configuration.getExtraProperties().size());
      for (SonarLintProperty prop : configuration.getExtraProperties()) {
        keyValuePairs.add(prop.getName() + "=" + prop.getValue());
      }
      String props = StringUtils.joinSkipNull(keyValuePairs, "\r\n");
      projectNode.put(P_EXTRA_PROPS, props);
    } else {
      projectNode.remove(P_EXTRA_PROPS);
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
      SonarLintCorePlugin.getDefault().error("Failed to save project configuration", e);
      return false;
    }
  }

}
