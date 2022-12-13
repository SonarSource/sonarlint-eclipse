/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2022 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.preferences;

import org.eclipse.core.runtime.preferences.IScopeContext;
import org.osgi.service.prefs.BackingStoreException;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration.EclipseProjectBinding;

import static org.sonarlint.eclipse.core.internal.utils.StringUtils.isBlank;
import static org.sonarlint.eclipse.core.internal.utils.StringUtils.isNotBlank;

public class SonarLintProjectConfigurationManager {

  private static final String P_EXTRA_PROPS = "extraProperties";
  private static final String P_FILE_EXCLUSIONS = "fileExclusions";
  public static final String P_SERVER_ID = "serverId";
  public static final String P_PROJECT_KEY = "projectKey";
  private static final String P_SQ_PREFIX_KEY = "sqPrefixKey";
  private static final String P_IDE_PREFIX_KEY = "idePrefixKey";
  /**
   * @deprecated since 3.7
   */
  @Deprecated
  private static final String P_MODULE_KEY = "moduleKey";
  private static final String P_AUTO_ENABLED_KEY = "autoEnabled";

  public SonarLintProjectConfiguration load(IScopeContext projectScope, String projectName) {
    var projectNode = projectScope.getNode(SonarLintCorePlugin.PLUGIN_ID);
    var projectConfig = new SonarLintProjectConfiguration();
    if (projectNode == null) {
      return projectConfig;
    }

    var extraArgsAsString = projectNode.get(P_EXTRA_PROPS, null);
    var sonarProperties = SonarLintGlobalConfiguration.deserializeExtraProperties(extraArgsAsString);
    var fileExclusionsAsString = projectNode.get(P_FILE_EXCLUSIONS, null);
    var fileExclusions = SonarLintGlobalConfiguration.deserializeFileExclusions(fileExclusionsAsString);

    projectConfig.getExtraProperties().addAll(sonarProperties);
    projectConfig.getFileExclusions().addAll(fileExclusions);
    var projectKey = projectNode.get(P_PROJECT_KEY, "");
    var moduleKey = projectNode.get(P_MODULE_KEY, "");
    if (isBlank(projectKey) && isNotBlank(moduleKey)) {
      SonarLintLogger.get().info("Binding configuration of project '" + projectName + "' is outdated. Please rebind this project.");
    }
    projectNode.remove(P_MODULE_KEY);
    var serverId = projectNode.get(P_SERVER_ID, "");
    if (isNotBlank(serverId) && isNotBlank(projectKey)) {
      projectConfig.setProjectBinding(new EclipseProjectBinding(serverId, projectKey, projectNode.get(P_SQ_PREFIX_KEY, ""), projectNode.get(P_IDE_PREFIX_KEY, "")));
    }
    projectConfig.setAutoEnabled(projectNode.getBoolean(P_AUTO_ENABLED_KEY, true));
    return projectConfig;
  }

  public void save(IScopeContext projectScope, SonarLintProjectConfiguration configuration) {
    var projectNode = projectScope.getNode(SonarLintCorePlugin.PLUGIN_ID);
    if (projectNode == null) {
      throw new IllegalStateException("Unable to get SonarLint settings node");
    }

    if (!configuration.getExtraProperties().isEmpty()) {
      var props = SonarLintGlobalConfiguration.serializeExtraProperties(configuration.getExtraProperties());
      projectNode.put(P_EXTRA_PROPS, props);
    } else {
      projectNode.remove(P_EXTRA_PROPS);
    }

    if (!configuration.getFileExclusions().isEmpty()) {
      var props = SonarLintGlobalConfiguration.serializeFileExclusions(configuration.getFileExclusions());
      projectNode.put(P_FILE_EXCLUSIONS, props);
    } else {
      projectNode.remove(P_FILE_EXCLUSIONS);
    }

    configuration.getProjectBinding().ifPresentOrElse(
      binding -> {
        projectNode.put(P_PROJECT_KEY, binding.projectKey());
        projectNode.put(P_SERVER_ID, binding.connectionId());
        projectNode.put(P_SQ_PREFIX_KEY, binding.serverPathPrefix());
        projectNode.put(P_IDE_PREFIX_KEY, binding.idePathPrefix());
      },
      () -> {
        projectNode.remove(P_PROJECT_KEY);
        projectNode.remove(P_SERVER_ID);
        projectNode.remove(P_SQ_PREFIX_KEY);
        projectNode.remove(P_IDE_PREFIX_KEY);
      });

    projectNode.putBoolean(P_AUTO_ENABLED_KEY, configuration.isAutoEnabled());
    try {
      projectNode.flush();
    } catch (BackingStoreException e) {
      SonarLintLogger.get().error("Failed to save project configuration", e);
    }
  }

}
