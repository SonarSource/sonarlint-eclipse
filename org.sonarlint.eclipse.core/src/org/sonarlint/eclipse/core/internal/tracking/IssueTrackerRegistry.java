/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2020 SonarSource SA
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

/**
 * Registry of per-module IssueTracker instances.
 */
public class IssueTrackerRegistry {

  // Use project name as key since we don't know if ISonarLintProject instances are implementing hashcode
  private final Map<String, IssueTracker> registry = new HashMap<>();
  private final IssueTrackerCacheFactory cacheFactory;

  public IssueTrackerRegistry(IssueTrackerCacheFactory cacheFactory) {
    this.cacheFactory = cacheFactory;
  }

  public synchronized IssueTracker getOrCreate(ISonarLintProject project) {
    IssueTracker tracker = registry.get(project.getName());
    if (tracker == null) {
      tracker = newTracker(project);
      registry.put(project.getName(), tracker);
    }
    return tracker;
  }

  public synchronized Optional<IssueTracker> get(ISonarLintProject project) {
    return Optional.ofNullable(registry.get(project.getName()));
  }

  private IssueTracker newTracker(ISonarLintProject project) {
    return new IssueTracker(cacheFactory.apply(project));
  }

  public void shutdown() {
    for (IssueTracker issueTracker : registry.values()) {
      issueTracker.shutdown();
    }
  }

}
