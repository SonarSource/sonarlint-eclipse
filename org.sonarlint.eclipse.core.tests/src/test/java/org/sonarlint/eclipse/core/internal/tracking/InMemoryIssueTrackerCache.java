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
package org.sonarlint.eclipse.core.internal.tracking;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryIssueTrackerCache implements IssueTrackerCache {

  private final Map<String, Collection<Trackable>> cache;

  public InMemoryIssueTrackerCache() {
    this.cache = new ConcurrentHashMap<>();
  }

  @Override
  public boolean isFirstAnalysis(String file) {
    return !cache.containsKey(file);
  }

  @Override
  public Collection<Trackable> getCurrentTrackables(String file) {
    return cache.get(file);
  }

  @Override
  public Collection<Trackable> getLiveOrFail(String file) {
    Collection<Trackable> trackables = cache.get(file);
    if (trackables != null) {
      return trackables;
    }
    throw new IllegalStateException("No trackables for file: " + file);
  }

  @Override
  public void put(String file, Collection<Trackable> trackables) {
    cache.put(file, trackables);
  }

  @Override
  public void clear() {
    cache.clear();
  }

  @Override
  public void shutdown() {
    // nothing to do
  }
}
