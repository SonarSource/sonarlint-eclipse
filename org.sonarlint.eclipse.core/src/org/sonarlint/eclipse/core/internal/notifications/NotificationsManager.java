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
package org.sonarlint.eclipse.core.internal.notifications;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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
    this(new Subscriber(), p -> SonarLintProjectConfiguration.read(p.getScopeContext()));
  }

  // only for testing
  public NotificationsManager(Subscriber subscriber, SonarLintProjectConfigurationReader configReader) {
    this.subscriber = subscriber;
    this.configReader = configReader;
  }

  public synchronized void subscribe(ISonarLintProject project, SonarQubeNotificationListener listener) {
    SonarLintProjectConfiguration config = configReader.apply(project);

    String projectKey = config.getProjectKey();
    if (projectKey == null) {
      return;
    }

    Set<String> moduleKeys = subscribers.get(projectKey);
    if (moduleKeys == null) {
      if (!subscriber.subscribe(project, config, listener)) {
        return;
      }
      moduleKeys = new HashSet<>();
      subscribers.put(projectKey, moduleKeys);
      listeners.put(projectKey, listener);
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
    SonarLintProjectConfiguration config = configReader.apply(project);

    String projectKey = config.getProjectKey();
    Set<String> moduleKeys = subscribers.get(projectKey);
    if (moduleKeys == null) {
      return;
    }

    String moduleKey = config.getModuleKey();
    moduleKeys.remove(moduleKey);

    if (moduleKeys.isEmpty()) {
      subscribers.remove(projectKey);
      subscriber.unsubscribe(listeners.remove(projectKey));
    }
  }

  // visible for testing
  public static class Subscriber {
    // visible for testing
    public boolean subscribe(ISonarLintProject project, SonarLintProjectConfiguration config, SonarQubeNotificationListener listener) {
      Server server = (Server) SonarLintCorePlugin.getServersManager().getServer(config.getServerId());
      if (server == null || !server.areNotificationsEnabled()) {
        return false;
      }

      LastNotificationTime notificationTime = new ProjectNotificationTime(project);

      NotificationConfiguration configuration = new NotificationConfiguration(listener, notificationTime, config.getProjectKey(), server.getConfig());
      SonarQubeNotifications.get().register(configuration);

      return true;
    }

    // visible for testing
    public void unsubscribe(SonarQubeNotificationListener listener) {
      SonarQubeNotifications.get().remove(listener);
    }
  }

  // visible for testing
  public interface SonarLintProjectConfigurationReader extends Function<ISonarLintProject, SonarLintProjectConfiguration> {
    SonarLintProjectConfiguration apply(ISonarLintProject project);
  }
}
