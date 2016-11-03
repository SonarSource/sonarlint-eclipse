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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class IssueTracker {

  private final IssueTrackerCache cache;
  private final TrackingChangeSubmitter changeSubmitter;

  public IssueTracker(IssueTrackerCache cache, TrackingChangeSubmitter changeSubmitter) {
    this.cache = cache;
    this.changeSubmitter = changeSubmitter;
  }

  /**
   * Match a new set of trackables to current state.
   * If this is the first analysis, leave creation date as null.
   *
   * @param file
   * @param trackables
   */
  public synchronized void matchAndTrackAsNew(String file, Collection<Trackable> trackables) {
    if (cache.isFirstAnalysis(file)) {
      updateTrackedIssues(file, trackables);
    } else {
      matchAndTrack(file, cache.getCurrentTrackables(file), trackables);
    }
  }

  /**
   * "Rebase" current trackables against given trackables.
   *
   * @param file
   * @param trackables
   */
  public synchronized void matchAndTrackAsBase(String file, Collection<Trackable> trackables) {
    Collection<Trackable> current = cache.getCurrentTrackables(file);
    if (current.isEmpty()) {
      // whatever is the base, if current is empty, then nothing to do
      return;
    }
    matchAndTrack(file, trackables, current);
  }

  // note: the base issues are type T: sometimes mutable, sometimes not (for example server issues)
  private void matchAndTrack(String file, Collection<Trackable> baseIssues, Collection<Trackable> nextIssues) {
    Collection<Trackable> trackedIssues = new ArrayList<>();
    Tracking<Trackable, Trackable> tracking = new Tracker<Trackable, Trackable>().track(() -> nextIssues, () -> baseIssues);
    for (Map.Entry<Trackable, Trackable> entry : tracking.getMatchedRaws().entrySet()) {
      Trackable next = new CombinedTrackable(entry.getValue(), entry.getKey());
      trackedIssues.add(next);
    }
    for (Trackable next : tracking.getUnmatchedRaws()) {
      if (next.getServerIssueKey() != null) {
        next = new DisconnectedTrackable(next);
      } else {
        next = new LeakedTrackable(next);
      }
      trackedIssues.add(next);
    }
    updateTrackedIssues(file, trackedIssues);
  }

  private void updateTrackedIssues(String file, Collection<Trackable> trackedIssues) {
    cache.put(file, trackedIssues);
    changeSubmitter.submit(file, trackedIssues);
  }

  public void clear() {
    cache.clear();
  }

  public void shutdown() {
    cache.shutdown();
  }
}
