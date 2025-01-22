/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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

import java.util.Set;
import java.util.function.Consumer;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.osgi.service.prefs.BackingStoreException;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration.EclipseProjectBinding;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

import static java.util.Optional.ofNullable;
import static org.sonarlint.eclipse.core.internal.utils.StringUtils.isNotBlank;

public class SonarLintProjectConfigurationManager {

  private static final String P_EXTRA_PROPS = "extraProperties";
  private static final String P_FILE_EXCLUSIONS = "fileExclusions";
  // Changing the node name would be a breaking change so we keep the old name "serverId" for now
  public static final String P_CONNECTION_ID = "serverId";
  public static final String P_PROJECT_KEY = "projectKey";
  /**
   * @deprecated since 10.0
   */
  @Deprecated(since = "10.0")
  private static final String P_SQ_PREFIX_KEY = "sqPrefixKey";
  /**
   * @deprecated since 10.0
   */
  @Deprecated(since = "10.0")
  private static final String P_IDE_PREFIX_KEY = "idePrefixKey";
  private static final String P_AUTO_ENABLED_KEY = "autoEnabled";
  public static final String P_BINDING_SUGGESTIONS_DISABLED_KEY = "bindingSuggestionsDisabled";
  public static final String P_INDEXING_BASED_ON_ECLIPSE_PLUGINS = "indexingBasedOnEclipsePlugIns";

  private static final Set<String> BINDING_RELATED_PROPERTIES = Set.of(P_PROJECT_KEY, P_CONNECTION_ID, P_BINDING_SUGGESTIONS_DISABLED_KEY);

  public static void registerPreferenceChangeListenerForBindingProperties(ISonarLintProject project, Consumer<ISonarLintProject> listener) {
    ofNullable(project.getScopeContext().getNode(SonarLintCorePlugin.PLUGIN_ID))
      .ifPresent(node -> {
        node.addPreferenceChangeListener(event -> {
          if (BINDING_RELATED_PROPERTIES.contains(event.getKey())) {
            listener.accept(project);
          }
        });
      });
  }

  public SonarLintProjectConfiguration load(IScopeContext projectScope) {
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
    var connectionId = projectNode.get(P_CONNECTION_ID, "");
    if (isNotBlank(connectionId) && isNotBlank(projectKey)) {
      projectConfig.setProjectBinding(new EclipseProjectBinding(connectionId, projectKey));
    }
    projectConfig.setAutoEnabled(projectNode.getBoolean(P_AUTO_ENABLED_KEY, true));
    projectConfig.setBindingSuggestionsDisabled(projectNode.getBoolean(P_BINDING_SUGGESTIONS_DISABLED_KEY, false));

    // When importing a project (but not when (re-)starting the workspace), and the project preferences are accessed
    // for the first time, they cannot be loaded and will fallback to the default values (in this case here "true").
    // This seems to be coming from Eclipse itself as the preferences are loaded lazily. This is no problem as when a
    // project is imported, right after importing, all files will be shown as changed (added) and then the preferences
    // will be read again (in FileSystemSynchronizer) and then they are fully available!
    // When starting a workspace and the project was already imported earlier, the preferences are not loaded lazily
    // anymore but fetched from cache and are not falling back to the default values - they are available at all times.
    projectConfig.setIndexingBasedOnEclipsePlugIns(projectNode.getBoolean(P_INDEXING_BASED_ON_ECLIPSE_PLUGINS, true));

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
        projectNode.put(P_PROJECT_KEY, binding.getProjectKey());
        projectNode.put(P_CONNECTION_ID, binding.getConnectionId());
      },
      () -> {
        projectNode.remove(P_PROJECT_KEY);
        projectNode.remove(P_CONNECTION_ID);
        projectNode.remove(P_SQ_PREFIX_KEY);
        projectNode.remove(P_IDE_PREFIX_KEY);
      });

    projectNode.putBoolean(P_AUTO_ENABLED_KEY, configuration.isAutoEnabled());
    projectNode.putBoolean(P_BINDING_SUGGESTIONS_DISABLED_KEY, configuration.isBindingSuggestionsDisabled());
    projectNode.putBoolean(P_INDEXING_BASED_ON_ECLIPSE_PLUGINS, configuration.isIndexingBasedOnEclipsePlugIns());
    try {
      projectNode.flush();
    } catch (BackingStoreException e) {
      SonarLintLogger.get().error("Failed to save project configuration", e);
    }
  }

}
