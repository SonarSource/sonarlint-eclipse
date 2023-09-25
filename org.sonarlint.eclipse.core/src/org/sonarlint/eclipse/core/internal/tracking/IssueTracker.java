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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;

public class IssueTracker {

  private final IssueTrackerCache cache;

  public IssueTracker(IssueTrackerCache cache) {
    this.cache = cache;
  }

  /**
   * Match a new set of trackables to current state.
   * If this is the first analysis, leave creation date as null.
   */
  public synchronized Collection<Trackable> matchAndTrackAsNew(ISonarLintFile file, Collection<Trackable> rawIssues) {
    Collection<Trackable> tracked;
    if (cache.isFirstAnalysis(file.getProjectRelativePath())) {
      tracked = rawIssues;
    } else {
      var trackedIssues = new ArrayList<Trackable>();
      var tracking = new Tracker<>().trackRaw(() -> rawIssues, () -> cache.getCurrentTrackables(file.getProjectRelativePath()));
      // Previous issues
      for (var entry : tracking.getMatchedRaws().entrySet()) {
        var next = new PreviousTrackable(entry.getValue(), entry.getKey());
        trackedIssues.add(next);
      }
      // New local issues compared to previous analysis
      for (var rawTrackable : tracking.getUnmatchedRaws()) {
        trackedIssues.add(new LeakedTrackable(rawTrackable));
      }
      tracked = trackedIssues;
    }
    return tracked;
  }

  public synchronized void updateCache(ISonarLintFile file, Collection<Trackable> tracked) {
    cache.put(file.getProjectRelativePath(), tracked);
  }
  
  public Collection<Trackable> getFromLocalCache(ISonarLintFile file) {
    return cache.getLiveOrFail(file.getProjectRelativePath());
  }

  public void clear() {
    cache.clear();
  }

  public void shutdown() {
    cache.shutdown();
  }
}
