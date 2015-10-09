/*
 * SonarLint for Eclipse
 * Copyright (C) 2015 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonarlint.eclipse.core.internal.resources;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.osgi.service.prefs.BackingStoreException;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;

public class SonarLintProjectManager {

  private static final String P_EXTRA_PROPS = "extraProperties";

  public SonarLintProject readSonarConfiguration(IProject project) {
    IScopeContext projectScope = new ProjectScope(project);
    IEclipsePreferences projectNode = projectScope.getNode(SonarLintCorePlugin.PLUGIN_ID);
    if (projectNode == null) {
      return new SonarLintProject(project);
    }

    SonarLintProject sonarProject = new SonarLintProject(project);
    String extraArgsAsString = projectNode.get(P_EXTRA_PROPS, null);
    List<SonarLintProperty> sonarProperties = new ArrayList<SonarLintProperty>();
    if (extraArgsAsString != null) {
      try {
        String[] props = StringUtils.split(extraArgsAsString, "\r\n");
        for (String keyValuePair : props) {
          String[] keyValue = StringUtils.split(keyValuePair, "=");
          sonarProperties.add(new SonarLintProperty(keyValue[0], keyValue.length > 1 ? keyValue[1] : ""));
        }
      } catch (Exception e) {
        SonarLintCorePlugin.getDefault().error("Error while loading SonarLint properties", e);
      }
    }
    sonarProject.setExtraProperties(sonarProperties);
    return sonarProject;
  }

  /**
   * @return false, if unable to save configuration
   */
  public boolean saveSonarConfiguration(IProject project, SonarLintProject configuration) {
    IScopeContext projectScope = new ProjectScope(project);
    IEclipsePreferences projectNode = projectScope.getNode(SonarLintCorePlugin.PLUGIN_ID);
    if (projectNode == null) {
      return false;
    }

    if (configuration.getExtraProperties() != null) {
      List<String> keyValuePairs = new ArrayList<String>(configuration.getExtraProperties().size());
      for (SonarLintProperty prop : configuration.getExtraProperties()) {
        keyValuePairs.add(prop.getName() + "=" + prop.getValue());
      }
      String props = StringUtils.join(keyValuePairs, "\r\n");
      projectNode.put(P_EXTRA_PROPS, props);
    } else {
      projectNode.remove(P_EXTRA_PROPS);
    }
    try {
      projectNode.flush();
      return true;
    } catch (BackingStoreException e) {
      SonarLintCorePlugin.getDefault().error("Failed to save project configuration", e);
      return false;
    }
  }

}
