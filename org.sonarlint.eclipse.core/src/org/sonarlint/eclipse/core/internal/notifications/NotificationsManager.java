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
package org.sonarlint.eclipse.core.internal.notifications;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectedEngineFacadeManager;
import org.sonarlint.eclipse.core.internal.engine.connected.IConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.engine.connected.ResolvedBinding;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.internal.resources.ProjectsProviderUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.serverconnection.smartnotifications.LastNotificationTime;
import org.sonarsource.sonarlint.core.serverconnection.smartnotifications.NotificationConfiguration;
import org.sonarsource.sonarlint.core.serverconnection.smartnotifications.ServerNotificationListener;
import org.sonarsource.sonarlint.core.serverconnection.smartnotifications.ServerNotificationsRegistry;

public class NotificationsManager {

  // project key -> ISonarLintProject names
  private final Map<String, Set<String>> subscribers = new HashMap<>();

  // project key -> listener
  private final Map<String, ServerNotificationListener> listeners = new HashMap<>();

  private final Function<ISonarLintProject, SonarLintProjectConfiguration> configReader;
  private final ServerNotificationsRegistry serverNotificationsRegistry;
  private final ConnectedEngineFacadeManager facadeManager;

  public NotificationsManager() {
    this(new ServerNotificationsRegistry(), SonarLintCorePlugin::loadConfig, SonarLintCorePlugin.getServersManager());
  }

  // only for testing
  public NotificationsManager(ServerNotificationsRegistry serverNotificationsRegistry, Function<ISonarLintProject, SonarLintProjectConfiguration> configReader,
    ConnectedEngineFacadeManager facadeManager) {
    this.configReader = configReader;
    this.serverNotificationsRegistry = serverNotificationsRegistry;
    this.facadeManager = facadeManager;
  }

  public void subscribeAllNeedingProjectsToNotifications(ListenerFactory listenerFactory) {
    try {
      subscribeToNotifications(ProjectsProviderUtils.allProjects(), listenerFactory);
    } catch (IllegalStateException e) {
      SonarLintLogger.get().error("Could not subscribe to notifications", e);
    }
  }

  public void subscribeToNotifications(Collection<ISonarLintProject> projects, ListenerFactory listenerFactory) {
    var projectsPerConnection = filterBoundProjectsAndGroupByConnection(projects);
    projectsPerConnection.forEach((facade, projectsToSubscribe) -> {
      if (((ConnectedEngineFacade) facade).checkNotificationsSupported()) {
        projectsToSubscribe.forEach(p -> subscribeBoundProject(p,
          facadeManager.resolveBinding(p).orElseThrow(() -> new IllegalStateException("Project was supposed to be bound")),
          listenerFactory.create(facade)));
      }
    });
  }

  private Map<IConnectedEngineFacade, List<ISonarLintProject>> filterBoundProjectsAndGroupByConnection(Collection<ISonarLintProject> projects) {
    var projectsPerConnection = new HashMap<IConnectedEngineFacade, List<ISonarLintProject>>();
    projects.forEach(p -> facadeManager.resolveBinding(p)
      .ifPresent(binding -> {
        if (!binding.getEngineFacade().areNotificationsDisabled()) {
          projectsPerConnection.computeIfAbsent(binding.getEngineFacade(), k -> new ArrayList<>()).add(p);
        }
      }));
    return projectsPerConnection;
  }

  /**
   * Read and save directly from the mutable object.
   * Any changes in the project settings will affect the next request.
   */
  // visible for testing
  public static class ProjectNotificationTime implements LastNotificationTime {

    private final NotificationsTracker tracker;

    // visible for testing
    public ProjectNotificationTime(NotificationsTracker tracker) {
      this.tracker = tracker;
    }

    public ProjectNotificationTime(ISonarLintProject project) {
      this(SonarLintCorePlugin.getOrCreateNotificationsTracker(project));
    }

    @Override
    public ZonedDateTime get() {
      return tracker.getLastEventPolling();
    }

    @Override
    public void set(ZonedDateTime dateTime) {
      tracker.updateLastEventPolling(dateTime);
    }
  }

  public synchronized void unsubscribe(ISonarLintProject project) {
    var config = configReader.apply(project);

    var binding = config.getProjectBinding();
    binding.ifPresent(b -> {
      var projectKey = b.projectKey();
      var names = subscribers.get(projectKey);
      if (names == null) {
        return;
      }

      names.remove(project.getName());

      if (names.isEmpty()) {
        subscribers.remove(projectKey);
        serverNotificationsRegistry.remove(listeners.remove(projectKey));
      }
    });
  }

  private synchronized void subscribeBoundProject(ISonarLintProject project, ResolvedBinding binding, ServerNotificationListener listener) {
    var projectKey = binding.getProjectBinding().projectKey();
    var names = subscribers.get(projectKey);
    if (names == null) {
      register(project, binding, listener);
      names = new HashSet<>();
      subscribers.put(projectKey, names);
      listeners.put(projectKey, listener);
    }
    names.add(project.getName());
  }

  private void register(ISonarLintProject project, ResolvedBinding binding, ServerNotificationListener listener) {
    var connectedEngineFacade = (ConnectedEngineFacade) binding.getEngineFacade();
    var notificationTime = new ProjectNotificationTime(project);

    var configuration = new NotificationConfiguration(listener, notificationTime, binding.getProjectBinding().projectKey(),
      connectedEngineFacade::createEndpointParams, connectedEngineFacade::buildClientWithProxyAndCredentials);
    serverNotificationsRegistry.register(configuration);
  }

  public void stop() {
    serverNotificationsRegistry.stop();
  }
}
