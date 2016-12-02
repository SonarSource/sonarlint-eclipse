/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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

import org.eclipse.core.resources.IProject;

/**
 * Registry of per-module IssueTracker instances.
 */
public class IssueTrackerRegistry {

  private final Map<IProject, IssueTracker> registry = new HashMap<>();
  private final IssueTrackerCacheFactory cacheFactory;

  public IssueTrackerRegistry(IssueTrackerCacheFactory cacheFactory) {
    this.cacheFactory = cacheFactory;
  }

  public synchronized IssueTracker getOrCreate(IProject project, String localModulePath) {
    IssueTracker tracker = registry.get(project);
    if (tracker == null) {
      tracker = newTracker(project, localModulePath);
      registry.put(project, tracker);
    }
    return tracker;
  }

  public synchronized Optional<IssueTracker> get(IProject project) {
    return Optional.ofNullable(registry.get(project));
  }

  private IssueTracker newTracker(IProject project, String localModulePath) {
    return new IssueTracker(cacheFactory.apply(project, localModulePath));
  }

  public void shutdown() {
    for (IssueTracker issueTracker : registry.values()) {
      issueTracker.shutdown();
    }
  }

}
