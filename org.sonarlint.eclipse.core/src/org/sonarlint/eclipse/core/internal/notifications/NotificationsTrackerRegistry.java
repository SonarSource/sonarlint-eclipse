/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2021 SonarSource SA
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

import java.util.HashMap;
import java.util.Map;
import org.sonarlint.eclipse.core.internal.StoragePathManager;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

/**
 * Registry of per-module NotificationsTracker instances.
 */
public class NotificationsTrackerRegistry {

  // Use project name as key since we don't know if ISonarLintProject instances are implementing hashcode
  private final Map<String, NotificationsTracker> registry = new HashMap<>();

  public synchronized NotificationsTracker getOrCreate(ISonarLintProject project) {
    String projectName = project.getName();
    NotificationsTracker tracker = registry.get(projectName);
    if (tracker == null) {
      tracker = newTracker(project);
      registry.put(projectName, tracker);
    }
    return tracker;
  }

  private static NotificationsTracker newTracker(ISonarLintProject project) {
    return new NotificationsTracker(StoragePathManager.getNotificationsDir(project));
  }
}
