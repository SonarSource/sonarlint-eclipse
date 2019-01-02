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
package org.sonarlint.eclipse.ui.internal.command;

import java.util.Collection;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.osgi.service.prefs.BackingStoreException;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProjectConfigurationManager;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

/**
 * Utility class to handle the toggling of project-specific settings and
 * persisting the in the projects preferences.
 *
 */
public class ToggleUtils {

  /**
   * 
   */
  private ToggleUtils() {
    // Utility class
  }

  /**
   * Entry function for updating the state of the type specified of the passed
   * list of projects.
   * 
   * @param selectedProjects
   * @param triggerType
   */
  public static void updateToggleState(Collection<ISonarLintProject> selectedProjects, TriggerType triggerType) {
    if (!selectedProjects.isEmpty()) {
      SonarLintLogger.get().debug("Found " + selectedProjects.size() + " selected projects...");

      // Loop through all the selected projects and update the state
      for (ISonarLintProject project : selectedProjects) {
        SonarLintLogger.get().debug("Updating analysis save trigger for - " + project.getName());
        updateToggleState(project, triggerType);
      }
    } else {
      SonarLintLogger.get().debug("No projects selected, no action will be performed");
    }
  }

  /**
   * Entry function for updating the state of the type specified of the passed
   * project.
   * 
   * @param project
   * @param triggerType
   */
  public static void updateToggleState(ISonarLintProject project, TriggerType triggerType) {
    SonarLintProjectConfiguration projectConfig = SonarLintCorePlugin.loadConfig(project);

    if (projectConfig != null) {
      boolean currentState = getCurrentSate(projectConfig, triggerType);
      IScopeContext projectScope = project.getScopeContext();
      IEclipsePreferences projectNode = projectScope.getNode(SonarLintCorePlugin.PLUGIN_ID);

      if (projectNode != null) {
        updateState(projectNode, currentState, triggerType);
      } else {
        SonarLintLogger.get().error(
            "Preferences for project (" + project.getName() + ") could not be determined, trigger will not be updated");
      }
    } else {
      SonarLintLogger.get().error("No configuration found for selected project (" + project.getName() + ")...");
    }
  }

  /**
   * Update the state of the toggle type by inversing the current state and
   * updating the Eclipse Preferences to persist the change.
   * 
   * @param projectNode
   * @param currentState
   * @param triggerType
   */
  private static void updateState(IEclipsePreferences projectNode, boolean currentState, TriggerType triggerType) {
    boolean newState = !currentState;
    String configKey = getConfigKey(triggerType);

    if (StringUtils.isNotBlank(configKey)) {
      try {
        // Update config item with new value and flush to backing store
        projectNode.putBoolean(configKey, newState);
        projectNode.flush();

        SonarLintLogger.get().info("Updated trigger state (" + triggerType.getName() + ") to " + newState);
      } catch (BackingStoreException e) {
        SonarLintLogger.get().error("Failed to persist trigger state (" + triggerType.getName() + ")", e);

        // Reset state back as persisting failed
        projectNode.putBoolean(configKey, currentState);
      }
    } else {
      SonarLintLogger.get().error("Persistent preference key not found for type - " + triggerType);
    }
  }

  /**
   * Get the associated configuration key used for specified toggle type.
   * 
   * @param triggerType
   * @return
   */
  private static String getConfigKey(TriggerType triggerType) {
    switch (triggerType) {
    case EDITOR_CHANGE:
      return SonarLintProjectConfigurationManager.P_TRIGGER_EDITOR_CHANGE;
    case EDITOR_OPEN:
      return SonarLintProjectConfigurationManager.P_TRIGGER_EDITOR_OPEN;
    default:
      return null;
    }
  }

  /**
   * Get the current boolean value of the type specified in the project
   * configuration scope.
   * 
   * @param projectConfig
   * @param triggerType
   * @return
   */
  private static boolean getCurrentSate(SonarLintProjectConfiguration projectConfig, TriggerType triggerType) {
    switch (triggerType) {
    case EDITOR_CHANGE:
      return projectConfig.isTriggerEditorChangeEnabled();
    case EDITOR_OPEN:
      return projectConfig.isTriggerEditorOpenEnabled();
    default:
      return false;
    }
  }

}
