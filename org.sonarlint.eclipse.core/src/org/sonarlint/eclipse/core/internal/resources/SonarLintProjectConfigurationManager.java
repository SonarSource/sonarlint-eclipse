/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2019 SonarSource SA
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
import org.sonarlint.eclipse.core.internal.resources.SonarLintProjectConfiguration.EclipseProjectBinding;
import org.sonarlint.eclipse.core.internal.utils.PreferencesUtils;

import static org.sonarlint.eclipse.core.internal.utils.StringUtils.isBlank;
import static org.sonarlint.eclipse.core.internal.utils.StringUtils.isNotBlank;

public class SonarLintProjectConfigurationManager {

  private static final String P_EXTRA_PROPS = "extraProperties";
  private static final String P_FILE_EXCLUSIONS = "fileExclusions";
  private static final String P_SERVER_ID = "serverId";
  private static final String P_PROJECT_KEY = "projectKey";
  private static final String P_SQ_PREFIX_KEY = "sqPrefixKey";
  private static final String P_IDE_PREFIX_KEY = "idePrefixKey";
  /**
   * @deprecated since 3.7
   */
  @Deprecated
  private static final String P_MODULE_KEY = "moduleKey";
  private static final String P_AUTO_ENABLED_KEY = "autoEnabled";

  public SonarLintProjectConfiguration load(IScopeContext projectScope) {
    IEclipsePreferences projectNode = projectScope.getNode(SonarLintCorePlugin.PLUGIN_ID);
    SonarLintProjectConfiguration projectConfig = new SonarLintProjectConfiguration();
    if (projectNode == null) {
      return projectConfig;
    }

    String extraArgsAsString = projectNode.get(P_EXTRA_PROPS, null);
    List<SonarLintProperty> sonarProperties = PreferencesUtils.deserializeExtraProperties(extraArgsAsString);
    String fileExclusionsAsString = projectNode.get(P_FILE_EXCLUSIONS, null);
    List<ExclusionItem> fileExclusions = PreferencesUtils.deserializeFileExclusions(fileExclusionsAsString);

    projectConfig.getExtraProperties().addAll(sonarProperties);
    projectConfig.getFileExclusions().addAll(fileExclusions);
    String projectKey = projectNode.get(P_PROJECT_KEY, "");
    String moduleKey = projectNode.get(P_MODULE_KEY, "");
    if (isBlank(projectKey) && isNotBlank(moduleKey)) {
      SonarLintLogger.get().info("Project preference " + projectScope.toString() + " is outdated. Please rebind this project.");
    }
    projectNode.remove(P_MODULE_KEY);
    String serverId = projectNode.get(P_SERVER_ID, "");
    if (isNotBlank(serverId) && isNotBlank(projectKey)) {
      projectConfig.setProjectBinding(new EclipseProjectBinding(serverId, projectKey, projectNode.get(P_SQ_PREFIX_KEY, ""), projectNode.get(P_IDE_PREFIX_KEY, "")));
    }
    projectConfig.setAutoEnabled(projectNode.getBoolean(P_AUTO_ENABLED_KEY, true));
    return projectConfig;
  }

  /**
   * @return false, if unable to save configuration
   */
  public boolean save(IScopeContext projectScope, SonarLintProjectConfiguration configuration) {
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

    if (configuration.getProjectBinding().isPresent()) {
      EclipseProjectBinding binding = configuration.getProjectBinding().get();
      projectNode.put(P_PROJECT_KEY, binding.projectKey());
      projectNode.put(P_SERVER_ID, binding.serverId());
      projectNode.put(P_SQ_PREFIX_KEY, binding.sqPathPrefix());
      projectNode.put(P_IDE_PREFIX_KEY, binding.idePathPrefix());
    } else {
      projectNode.remove(P_PROJECT_KEY);
      projectNode.remove(P_SERVER_ID);
      projectNode.remove(P_SQ_PREFIX_KEY);
      projectNode.remove(P_IDE_PREFIX_KEY);
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
