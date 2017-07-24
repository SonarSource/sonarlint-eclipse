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

  private final Subscriber subscriber;
  private final SonarLintProjectConfigurationReader configReader;

  public NotificationsManager() {
    this(new Subscriber(), new SonarLintProjectConfigurationReader());
  }

  // only for testing
  public NotificationsManager(Subscriber subscriber, SonarLintProjectConfigurationReader configReader) {
    this.subscriber = subscriber;
    this.configReader = configReader;
  }

  // only for testing
  public int getSubscriberCount() {
    return subscribers.size();
  }

  public synchronized void subscribe(ISonarLintProject project, SonarQubeNotificationListener listener) {
    SonarLintProjectConfiguration config = configReader.read(project);

    String projectKey = config.getProjectKey();
    Set<String> moduleKeys = subscribers.get(projectKey);
    if (moduleKeys == null) {
      moduleKeys = new HashSet<>();
      subscribers.put(projectKey, moduleKeys);
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
  // visible for testing
  public static class ProjectNotificationTime implements LastNotificationTime {

    final ProjectState projectState = new ProjectState();

    // TODO make this persist when changed, load from storage when started
    static class ProjectState {
      ZonedDateTime lastEventPolling;

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

  // visible for testing
  public static class Subscriber {
    // visible for testing
    public void subscribe(SonarLintProjectConfiguration config, SonarQubeNotificationListener listener) {
      LastNotificationTime notificationTime = new ProjectNotificationTime();

      Server server = (Server) SonarLintCorePlugin.getServersManager().getServer(config.getServerId());

      NotificationConfiguration configuration = new NotificationConfiguration(listener, notificationTime, config.getProjectKey(), server.getConfig());
      SonarQubeNotifications.get().register(configuration);
    }

    // visible for testing
    public void unsubscribe(SonarQubeNotificationListener listener) {
      SonarQubeNotifications.get().remove(listener);
    }
  }

  // visible for testing
  public static class SonarLintProjectConfigurationReader {
    // visible for testing
    public SonarLintProjectConfiguration read(ISonarLintProject project) {
      SonarLintProjectConfiguration config = SonarLintProjectConfiguration.read(project.getScopeContext());
      // TODO remove this temporary hack (this helps testing with single-module projects)
      if (config.getProjectKey() == null) {
        config.setProjectKey(config.getModuleKey());
      }
      return config;
    }
  }
}
