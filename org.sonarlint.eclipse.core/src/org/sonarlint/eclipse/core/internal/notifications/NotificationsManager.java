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
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.client.api.common.NotificationConfiguration;
import org.sonarsource.sonarlint.core.client.api.notifications.LastNotificationTime;
import org.sonarsource.sonarlint.core.client.api.notifications.SonarQubeNotification;
import org.sonarsource.sonarlint.core.client.api.notifications.SonarQubeNotificationListener;
import org.sonarsource.sonarlint.core.notifications.SonarQubeNotifications;

public class NotificationsManager {

  // project key -> module keys
  private final Map<String, Set<String>> subscribers = new HashMap<>();

  private final Map<String, SonarQubeNotificationListener> listeners = new HashMap<>();

  public synchronized void subscribe(ISonarLintProject project) {
    String projectKey = getProjectKey(project);
    Set<String> moduleKeys = subscribers.get(projectKey);
    if (moduleKeys == null) {
      moduleKeys = new HashSet<>();
      subscribers.put(projectKey, moduleKeys);
      SonarQubeNotificationListener listener = newListener();
      listeners.put(projectKey, listener);
      subscribe(project, projectKey, listener);
    }
    
    String moduleKey = getModuleKey(project);
    moduleKeys.add(moduleKey);
  }

  private void subscribe(ISonarLintProject project, String projectKey, SonarQubeNotificationListener listener) {
    LastNotificationTime notificationTime = new ProjectNotificationTime();
    
    SonarLintProjectConfiguration config = SonarLintProjectConfiguration.read(project.getScopeContext());
    Server server = (Server) SonarLintCorePlugin.getServersManager().getServer(config.getServerId());

    NotificationConfiguration configuration = new NotificationConfiguration(listener, notificationTime, projectKey, server.getConfig());
    SonarQubeNotifications.get().register(configuration);
  }

  // TODO inject in constructor for testing
  private SonarQubeNotificationListener newListener() {
    return new SonarQubeNotificationListener() {
      @Override
      public void handle(SonarQubeNotification notification) {
        System.out.println(notification.message());
        System.out.println(notification.link());
      }
    };
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
    String projectKey = getProjectKey(project);
    Set<String> moduleKeys = subscribers.get(projectKey);
    if (moduleKeys == null) {
      return;
    }
    
    String moduleKey = getModuleKey(project);
    moduleKeys.remove(moduleKey);
    
    if (moduleKeys.isEmpty()) {
      subscribers.remove(projectKey);
      unsubscribe(listeners.get(projectKey));
    }
  }
  
  private void unsubscribe(SonarQubeNotificationListener listener) {
    SonarQubeNotifications.get().remove(listener);
  }

  private String getModuleKey(ISonarLintProject project) {
    return SonarLintProjectConfiguration.read(project.getScopeContext()).getModuleKey();
  }

  private String getProjectKey(ISonarLintProject project) {
    String projectKey = SonarLintProjectConfiguration.read(project.getScopeContext()).getProjectKey();
    // TODO remove before release
    String dummyProjectKey = "org.sonarsource.scanner.cli:sonar-scanner-cli";
    return StringUtils.isEmpty(projectKey) ? dummyProjectKey : projectKey;
  }
}
