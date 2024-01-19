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
package org.sonarlint.eclipse.core.internal.tracking;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.runtime.Adapters;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

/**
 * Registry of per-project IssueTracker instances.
 */
public class ProjectIssueTrackers implements IResourceChangeListener {
  private final Map<ISonarLintProject, ProjectIssueTracker> registry = new ConcurrentHashMap<>();

  public ProjectIssueTracker getOrCreate(ISonarLintProject project) {
    return registry.computeIfAbsent(project, ProjectIssueTracker::new);
  }

  public Optional<ProjectIssueTracker> get(ISonarLintProject project) {
    return Optional.ofNullable(registry.get(project));
  }

  public void shutdown() {
    for (ProjectIssueTracker issueTracker : registry.values()) {
      issueTracker.flushAll();
    }
  }

  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    if (event.getType() == IResourceChangeEvent.PRE_CLOSE || event.getType() == IResourceChangeEvent.PRE_DELETE) {
      var project = Adapters.adapt(event.getResource(), ISonarLintProject.class);
      if (project != null) {
        var removed = registry.remove(project);
        if (removed != null) {
          removed.flushAll();
        }
      }
    }
  }
}
