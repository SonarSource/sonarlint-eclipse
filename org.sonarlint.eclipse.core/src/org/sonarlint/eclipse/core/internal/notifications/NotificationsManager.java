/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2017 SonarSource SA
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.internal.server.Server;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.client.api.common.NotificationConfiguration;
import org.sonarsource.sonarlint.core.client.api.notifications.LastNotificationTime;
import org.sonarsource.sonarlint.core.client.api.notifications.SonarQubeNotificationListener;
import org.sonarsource.sonarlint.core.notifications.SonarQubeNotifications;

public class NotificationsManager {

  // project key -> module keys
  private final Map<String, Set<String>> subscribers = new HashMap<>();

  // project key -> listener
  private final Map<String, SonarQubeNotificationListener> listeners = new HashMap<>();

  private final ListenerFactory listenerFactory;
  private final Subscriber subscriber;
  private final SonarLintProjectConfigurationReader configReader;

  public NotificationsManager(ListenerFactory listenerFactory) {
    this(listenerFactory, new Subscriber(), new SonarLintProjectConfigurationReader());
  }

  // only for testing
  NotificationsManager(ListenerFactory listenerFactory, Subscriber subscriber, SonarLintProjectConfigurationReader configReader) {
    this.listenerFactory = listenerFactory;
    this.subscriber = subscriber;
    this.configReader = configReader;
  }

  // only for testing
  int getSubscriberCount() {
    return subscribers.size();
  }

  public synchronized void subscribe(ISonarLintProject project) {
    SonarLintProjectConfiguration config = configReader.read(project);

    String projectKey = config.getProjectKey();
    Set<String> moduleKeys = subscribers.get(projectKey);
    if (moduleKeys == null) {
      moduleKeys = new HashSet<>();
      subscribers.put(projectKey, moduleKeys);
      SonarQubeNotificationListener listener = listenerFactory.create();
      listeners.put(projectKey, listener);
      subscriber.subscribe(config, listener);
    }

    String moduleKey = config.getModuleKey();
    moduleKeys.add(moduleKey);
  }

  /**
   * Read and save directly from the mutable object.
   * Any changes in the project settings will affect the next request.
   */
  private static class ProjectNotificationTime implements LastNotificationTime {

    final ProjectState projectState = new ProjectState();

    // TODO make this persist when changed, load from storage when started
    static class ProjectState {
      ZonedDateTime lastEventPolling = ZonedDateTime.now();

      ZonedDateTime getLastEventPolling() {
        return lastEventPolling;
      }

      void setLastEventPolling(ZonedDateTime time) {
        lastEventPolling = time;
      }
    }

    @Override
    public ZonedDateTime get() {
      ZonedDateTime lastEventPolling = projectState.getLastEventPolling();
      if (lastEventPolling == null) {
        lastEventPolling = ZonedDateTime.now();
        projectState.setLastEventPolling(lastEventPolling);
      }
      return lastEventPolling;
    }

    @Override
    public void set(ZonedDateTime dateTime) {
      // this could be false if the settings changed between the read and write
      if (dateTime.isAfter(projectState.getLastEventPolling())) {
        projectState.setLastEventPolling(dateTime);
      }
    }
  }

  public synchronized void unsubscribe(ISonarLintProject project) {
    SonarLintProjectConfiguration config = configReader.read(project);

    String projectKey = config.getProjectKey();
    Set<String> moduleKeys = subscribers.get(projectKey);
    if (moduleKeys == null) {
      return;
    }

    String moduleKey = config.getModuleKey();
    moduleKeys.remove(moduleKey);

    if (moduleKeys.isEmpty()) {
      subscribers.remove(projectKey);
      subscriber.unsubscribe(listeners.get(projectKey));
    }
  }

  static class Subscriber {
    void subscribe(SonarLintProjectConfiguration config, SonarQubeNotificationListener listener) {
      LastNotificationTime notificationTime = new ProjectNotificationTime();

      Server server = (Server) SonarLintCorePlugin.getServersManager().getServer(config.getServerId());

      NotificationConfiguration configuration = new NotificationConfiguration(listener, notificationTime, config.getProjectKey(), server.getConfig());
      SonarQubeNotifications.get().register(configuration);
    }

    void unsubscribe(SonarQubeNotificationListener listener) {
      SonarQubeNotifications.get().remove(listener);
    }
  }

  static class SonarLintProjectConfigurationReader {
    SonarLintProjectConfiguration read(ISonarLintProject project) {
      return SonarLintProjectConfiguration.read(project.getScopeContext());
    }
  }
}
