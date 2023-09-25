/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2023 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.tracking;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.runtime.Adapters;
import org.sonarlint.eclipse.core.internal.StoragePathManager;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

/**
 * Registry of per-project IssueTracker instances.
 */
public class IssueTrackerRegistry implements IResourceChangeListener {
  private final Map<ISonarLintProject, IssueTracker> registry = new ConcurrentHashMap<>();

  public IssueTracker getOrCreate(ISonarLintProject project) {
    return registry.computeIfAbsent(project, p -> newTracker(p));
  }

  public Optional<IssueTracker> get(ISonarLintProject project) {
    return Optional.ofNullable(registry.get(project));
  }

  private static IssueTracker newTracker(ISonarLintProject project) {
    var storeBasePath = StoragePathManager.getIssuesDir(project);
    var issueStore = new IssueStore(storeBasePath, project);
    var trackerCache = new PersistentIssueTrackerCache(issueStore);
    
    return new IssueTracker(trackerCache);
  }

  public void shutdown() {
    for (IssueTracker issueTracker : registry.values()) {
      issueTracker.shutdown();
    }
  }

  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    if (event.getType() == IResourceChangeEvent.PRE_CLOSE || event.getType() == IResourceChangeEvent.PRE_DELETE) {
      var project = Adapters.adapt(event.getResource(), ISonarLintProject.class);
      if (project != null) {
        var removed = registry.remove(project);
        removed.shutdown();
      }
    }
  }
}
