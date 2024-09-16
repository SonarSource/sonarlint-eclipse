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
package org.sonarlint.eclipse.buildship.internal;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.Nullable;
import org.gradle.tooling.ProjectConnection;

/**
 *  For Gradle we rely on the Eclipse Buildship plug-in that contains the Gradle Tooling API bundled with it. We have
 *  to bypass the Eclipse plug-in and directly connect to the Gradle "runtime" which is quite costly and not needed
 *  that often except for indexing (ISonarLintProject#files() / FileSystemSynchronizer).
 *
 *  Having a cache for the connections that are linked to one or multiple configuration scope ids is necessary to lower
 *  the time it takes for projects to index and perform
 */
public class ProjectConnectionCache {
  // Multiple configuration scope ids can share one connection, e.g. multi project builds where there is one root
  // project and multiple sub-projects that are independent in Eclipse due to the flat hierarchy.
  private static final ConcurrentHashMap<List<String>, ProjectConnection> cache = new ConcurrentHashMap<>();

  private ProjectConnectionCache() {
    // utility class
  }

  @Nullable
  public static ProjectConnection getConnection(String configScopeId) {
    for (var entry : cache.entrySet()) {
      if (entry.getKey().contains(configScopeId)) {
        return entry.getValue();
      }
    }
    return null;
  }

  // Big projects can take up to a minute to load (e.g. SonarQube takes 30 seconds at most but only has 360 kLOC) and
  // the Gradle project information likely won't change in that time while the project is loaded.
  public static void putConnection(List<String> configScopeIds, ProjectConnection connection) {
    cache.put(configScopeIds, connection);
    new RemoveConnectionJob(configScopeIds)
      .schedule(60000);
  }

  public static void removeConnection(List<String> configScopeIds) {
    cache.remove(configScopeIds);
  }

  private static class RemoveConnectionJob extends Job {
    private final List<String> configScopeIds;

    public RemoveConnectionJob(List<String> configScopeIds) {
      // INFO: We use the hash here as more or less an "id" of the job, meaningless otherwise!
      super("Delete cache entry for: " + configScopeIds.hashCode());
      this.configScopeIds = configScopeIds;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      removeConnection(configScopeIds);
      return Status.OK_STATUS;
    }
  }
}
