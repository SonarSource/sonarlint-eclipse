/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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
import org.eclipse.core.runtime.CoreException;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration.EclipseProjectBinding;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfigurationManager;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.rpc.protocol.SonarLintRpcServer;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.config.binding.BindingConfigurationDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.config.binding.DidUpdateBindingParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.config.scope.ConfigurationScopeDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.config.scope.DidAddConfigurationScopesParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.config.scope.DidRemoveConfigurationScopeParams;

import static java.util.stream.Collectors.toList;

public class ConfigScopeSynchronizer implements IResourceChangeListener {

  private final SonarLintRpcServer backend;

  ConfigScopeSynchronizer(SonarLintRpcServer backend) {
    this.backend = backend;
  }

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
      backend.getConfigurationService().didAddConfigurationScopes(new DidAddConfigurationScopesParams(addedScopes));
      projectsToAdd.forEach(p -> SonarLintProjectConfigurationManager.registerPreferenceChangeListenerForBindingProperties(p, this::projectPreferencesChanged));
    } else if (event.getType() == IResourceChangeEvent.PRE_CLOSE) {
      var project = SonarLintUtils.adapt(event.getResource(), ISonarLintProject.class,
        "[ConfigScopeSynchronizer#resourceChanged] Try get SonarLint project from event '" + event.getResource()
          + "' (pre close)");
      if (project != null) {
        SonarLintLogger.get().debug("Project about to be closed: " + project.getName());
        backend.getConfigurationService()
          .didRemoveConfigurationScope(new DidRemoveConfigurationScopeParams(getConfigScopeId(project)));
      }
    } else if (event.getType() == IResourceChangeEvent.PRE_DELETE) {
      var project = SonarLintUtils.adapt(event.getResource(), ISonarLintProject.class,
        "[ConfigScopeSynchronizer#resourceChanged] Try get SonarLint project from event '" + event.getResource()
          + "' (pre delete)");
      if (project != null) {
        SonarLintLogger.get().debug("Project about to be deleted: " + project.getName());
        backend.getConfigurationService()
          .didRemoveConfigurationScope(new DidRemoveConfigurationScopeParams(getConfigScopeId(project)));
      }
    }
  }

  private static boolean visitDeltaPostChange(IResourceDelta delta, List<ISonarLintProject> projectsToAdd) {
    if ((delta.getFlags() & IResourceDelta.OPEN) != 0) {
      var project = SonarLintUtils.adapt(delta.getResource(), ISonarLintProject.class,
        "[ConfigScopeSynchronizer#resourceChanged] Try get SonarLint project from event '"
          + delta.getResource() + "' (post change / opened)");
      if (project != null && project.isOpen()) {
        SonarLintLogger.get().debug("Project opened: " + project.getName());
        projectsToAdd.add(project);
      }
      return false;
    }
    return true;
  }

  public void init() {
    var allProjects = SonarLintUtils.allProjects();
    var initialConfigScopes = allProjects.stream()
      .filter(ISonarLintProject::isOpen)
      .map(ConfigScopeSynchronizer::toConfigScopeDto)
      .collect(toList());
    backend.getConfigurationService().didAddConfigurationScopes(new DidAddConfigurationScopesParams(initialConfigScopes));
    allProjects.forEach(p -> {
      SonarLintProjectConfigurationManager.registerPreferenceChangeListenerForBindingProperties(p, this::projectPreferencesChanged);
    });
  }

  private void projectPreferencesChanged(ISonarLintProject project) {
    SonarLintLogger.get().debug("Project binding preferences changed: " + project.getName());
    backend.getConfigurationService()
      .didUpdateBinding(new DidUpdateBindingParams(getConfigScopeId(project), toBindingDto(project)));
  }

  private static ConfigurationScopeDto toConfigScopeDto(ISonarLintProject p) {
    return new ConfigurationScopeDto(getConfigScopeId(p), null, true, p.getName(), toBindingDto(p));
  }

  public static String getConfigScopeId(ISonarLintProject p) {
    return p.getResource().getLocationURI().toString();
  }

  private static BindingConfigurationDto toBindingDto(ISonarLintProject p) {
    return toBindingDto(p, false);
  }

  private static BindingConfigurationDto toBindingDto(ISonarLintProject p, boolean disableBindingSuggestions) {
    var config = SonarLintCorePlugin.loadConfig(p);
    var projectBinding = config.getProjectBinding();
    return new BindingConfigurationDto(projectBinding.map(EclipseProjectBinding::getConnectionId).orElse(null),
      projectBinding.map(EclipseProjectBinding::getProjectKey).orElse(null),
      disableBindingSuggestions || config.isBindingSuggestionsDisabled());
  }

  /**
   *  It might be useful to disable the binding suggestions for a specific project (e.g. Connection creation with
   *  directly followed binding) no matter what the configuration of the specific project is on that matter.
   */
  public static void disableAllBindingSuggestions(ISonarLintProject p) {
    SonarLintBackendService.get()
      .getBackend()
      .getConfigurationService()
      .didUpdateBinding(new DidUpdateBindingParams(getConfigScopeId(p), toBindingDto(p, true)));
  }

  /**
   *  After the binding suggestions for a specific project were disabled, we want to disable them again based on the
   *  project configuration (to not enable suggestions for project that disabled them manually).
   */
  public static void enableAllBindingSuggestions(ISonarLintProject p) {
    SonarLintBackendService.get()
      .getBackend()
      .getConfigurationService()
      .didUpdateBinding(new DidUpdateBindingParams(getConfigScopeId(p), toBindingDto(p, false)));
  }
}
