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
package org.sonarlint.eclipse.core.internal.backend;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration.EclipseProjectBinding;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfigurationManager;
import org.sonarlint.eclipse.core.internal.resources.ProjectsProviderUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.clientapi.backend.config.binding.BindingConfigurationDto;
import org.sonarsource.sonarlint.core.clientapi.backend.config.binding.DidUpdateBindingParams;
import org.sonarsource.sonarlint.core.clientapi.backend.config.scope.ConfigurationScopeDto;
import org.sonarsource.sonarlint.core.clientapi.backend.config.scope.DidAddConfigurationScopesParams;
import org.sonarsource.sonarlint.core.clientapi.backend.config.scope.DidRemoveConfigurationScopeParams;

import static java.util.stream.Collectors.toList;

public class ConfigScopeSynchronizer implements IResourceChangeListener {

  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
      var projectsToAdd = new ArrayList<ISonarLintProject>();
      try {
        event.getDelta().accept(delta -> visitDeltaPostChange(delta, projectsToAdd));
      } catch (CoreException e) {
        SonarLintLogger.get().error(e.getMessage(), e);
      }
      var addedScopes = projectsToAdd.stream()
        .map(ConfigScopeSynchronizer::toConfigScopeDto)
        .collect(toList());
      SonarLintBackendService.get().getBackend().getConfigurationService().didAddConfigurationScopes(new DidAddConfigurationScopesParams(addedScopes));
      projectsToAdd.forEach(p -> SonarLintProjectConfigurationManager.registerPreferenceChangeListenerForBindingProperties(p, ConfigScopeSynchronizer::projectPreferencesChanged));
    } else if (event.getType() == IResourceChangeEvent.PRE_CLOSE) {
      var project = Adapters.adapt(event.getResource(), ISonarLintProject.class);
      if (project != null) {
        SonarLintLogger.get().debug("Project about to be closed: " + project.getName());
        SonarLintBackendService.get().getBackend().getConfigurationService()
          .didRemoveConfigurationScope(new DidRemoveConfigurationScopeParams(getConfigScopeId(project)));
      }
    } else if (event.getType() == IResourceChangeEvent.PRE_DELETE) {
      var project = Adapters.adapt(event.getResource(), ISonarLintProject.class);
      if (project != null) {
        SonarLintLogger.get().debug("Project about to be deleted: " + project.getName());
        SonarLintBackendService.get().getBackend().getConfigurationService()
          .didRemoveConfigurationScope(new DidRemoveConfigurationScopeParams(getConfigScopeId(project)));
      }
    }
  }

  private static boolean visitDeltaPostChange(IResourceDelta delta, List<ISonarLintProject> projectsToAdd) {
    if ((delta.getFlags() & IResourceDelta.OPEN) != 0) {
      var project = Adapters.adapt(delta.getResource(), ISonarLintProject.class);
      if (project != null && project.isOpen()) {
        SonarLintLogger.get().debug("Project opened: " + project.getName());
        projectsToAdd.add(project);
      }
      return false;
    }
    return true;
  }

  public void init() {
    var allProjects = ProjectsProviderUtils.allProjects();
    var initialConfigScopes = allProjects.stream()
      .filter(ISonarLintProject::isOpen)
      .map(ConfigScopeSynchronizer::toConfigScopeDto)
      .collect(toList());
    SonarLintBackendService.get().getBackend().getConfigurationService().didAddConfigurationScopes(new DidAddConfigurationScopesParams(initialConfigScopes));
    allProjects.forEach(p -> SonarLintProjectConfigurationManager.registerPreferenceChangeListenerForBindingProperties(p, ConfigScopeSynchronizer::projectPreferencesChanged));
  }

  private static void projectPreferencesChanged(ISonarLintProject project) {
    SonarLintLogger.get().debug("Project binding preferences changed: " + project.getName());
    SonarLintBackendService.get().getBackend().getConfigurationService()
      .didUpdateBinding(new DidUpdateBindingParams(getConfigScopeId(project), toBindingDto(project)));
  }

  private static ConfigurationScopeDto toConfigScopeDto(ISonarLintProject p) {
    return new ConfigurationScopeDto(getConfigScopeId(p), null, true, p.getName(), toBindingDto(p));
  }

  static String getConfigScopeId(ISonarLintProject p) {
    return p.getResource().getLocationURI().toString();
  }

  private static BindingConfigurationDto toBindingDto(ISonarLintProject p) {
    var config = SonarLintCorePlugin.loadConfig(p);
    var projectBinding = config.getProjectBinding();
    return new BindingConfigurationDto(projectBinding.map(EclipseProjectBinding::connectionId).orElse(null), projectBinding.map(EclipseProjectBinding::projectKey).orElse(null),
      config.isBindingSuggestionsDisabled());
  }
}
