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
package org.sonarlint.eclipse.core.internal.notifications;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProjectConfiguration.EclipseProjectBinding;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.core.internal.server.Server;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.client.api.common.NotificationConfiguration;
import org.sonarsource.sonarlint.core.client.api.notifications.LastNotificationTime;
import org.sonarsource.sonarlint.core.client.api.notifications.SonarQubeNotificationListener;
import org.sonarsource.sonarlint.core.notifications.SonarQubeNotifications;

public class NotificationsManager {

  // project key -> ISonarLintProject names
  private final Map<String, Set<String>> subscribers = new HashMap<>();

  // project key -> listener
  private final Map<String, SonarQubeNotificationListener> listeners = new HashMap<>();

  private final Subscriber subscriber;
  private final SonarLintProjectConfigurationReader configReader;

  public NotificationsManager() {
    this(new Subscriber(), SonarLintCorePlugin::loadConfig);
  }

  // only for testing
  public NotificationsManager(Subscriber subscriber, SonarLintProjectConfigurationReader configReader) {
    this.subscriber = subscriber;
    this.configReader = configReader;
  }

  public synchronized void subscribe(ISonarLintProject project, SonarQubeNotificationListener listener) {
    SonarLintProjectConfiguration config = configReader.apply(project);

    Optional<EclipseProjectBinding> binding = config.getProjectBinding();
    if (!binding.isPresent()) {
      return;
    }
    binding.ifPresent(b -> {
      Set<String> names = subscribers.get(b.projectKey());
      if (names == null) {
        if (!subscriber.subscribe(project, config, listener)) {
          return;
        }
        names = new HashSet<>();
        subscribers.put(b.projectKey(), names);
        listeners.put(b.projectKey(), listener);
      }
      names.add(project.getName());
    });

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

    Optional<EclipseProjectBinding> binding = config.getProjectBinding();
    binding.ifPresent(b -> {
      String projectKey = b.projectKey();
      Set<String> names = subscribers.get(projectKey);
      if (names == null) {
        return;
      }

      names.remove(project.getName());

      if (names.isEmpty()) {
        subscribers.remove(projectKey);
        subscriber.unsubscribe(listeners.remove(projectKey));
      }
    });
  }

  public static class Subscriber {
    public boolean subscribe(ISonarLintProject project, SonarLintProjectConfiguration config, SonarQubeNotificationListener listener) {
      Optional<IServer> server = SonarLintCorePlugin.getServersManager().forProject(project, config);
      if (!server.isPresent() || !server.get().areNotificationsEnabled()) {
        return false;
      }

      LastNotificationTime notificationTime = new ProjectNotificationTime(project);

      NotificationConfiguration configuration = new NotificationConfiguration(listener, notificationTime, config.getProjectBinding().get().serverId(),
        ((Server) server.get()).getConfig());
      SonarQubeNotifications.get().register(configuration);

      return true;
    }

    public void unsubscribe(SonarQubeNotificationListener listener) {
      SonarQubeNotifications.get().remove(listener);
    }
  }

  // visible for testing
  public interface SonarLintProjectConfigurationReader extends Function<ISonarLintProject, SonarLintProjectConfiguration> {
    SonarLintProjectConfiguration apply(ISonarLintProject project);
  }
}
