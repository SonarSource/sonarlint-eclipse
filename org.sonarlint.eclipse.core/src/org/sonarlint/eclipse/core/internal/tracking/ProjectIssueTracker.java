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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.IProgressMonitor;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.StoragePathManager;
import org.sonarlint.eclipse.core.internal.backend.SonarLintBackendService;
import org.sonarlint.eclipse.core.internal.tracking.matching.IssueMatcher;
import org.sonarlint.eclipse.core.internal.tracking.matching.MatchableIssue;
import org.sonarlint.eclipse.core.internal.utils.JobUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintIssuable;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.tracking.ClientTrackedFindingDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.tracking.LineWithHashDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.tracking.TextRangeWithHashDto;

/**
 * Keep track of all issues for a given (Eclipse) project
 */
public class ProjectIssueTracker {

  private final PersistentLocalIssueStore store;
  private final Map<String, Collection<TrackedIssue>> trackedIssuesPerRelativePath = new HashMap<>();
  private final ISonarLintProject project;

  public ProjectIssueTracker(ISonarLintProject project) {
    this(project, new PersistentLocalIssueStore(StoragePathManager.getIssuesDir(project), project));
  }

  /**
   * Used for testing
   */
  public ProjectIssueTracker(ISonarLintProject project, PersistentLocalIssueStore store) {
    this.project = project;
    this.store = store;
  }

  private boolean isFirstAnalysis(String file) {
    return !trackedIssuesPerRelativePath.containsKey(file) && !store.contains(file);
  }

  /**
   * Process the result of a fresh analysis.
   */
  public synchronized void processRawIssues(ISonarLintFile file, Collection<RawIssueTrackable> rawIssues) {
    if (isFirstAnalysis(file.getProjectRelativePath())) {
      // We have never analyzed this file before, so we start tracking all issues with an unknown creation date
      trackAllRawIssuesAsUnknownCreationDate(file, rawIssues);
    } else {
      var trackedIssues = trackedIssuesPerRelativePath.get(file.getProjectRelativePath());
      if (trackedIssues != null) {
        // We have already analyzed this file since the ProjectIssueTracker has been created
        Collection<MatchableIssue> rawIssuesMatchable = rawIssues.stream().map(r -> r).collect(Collectors.toList());
        var matchingResult = IssueMatcher.matchIssues(rawIssuesMatchable, trackedIssues);
        // Update matching issues based on fresh raw issues
        for (var entry : matchingResult.getMatchedRaws().entrySet()) {
          var previous = (TrackedIssue) entry.getValue();
          var raw = (RawIssueTrackable) entry.getKey();
          previous.updateFromFreshAnalysis(raw);
        }
        // New raw issues compared to previous tracked issues
        for (var rawTrackable : matchingResult.getUnmatchedLefts()) {
          trackedIssues.add(TrackedIssue.asNew((RawIssueTrackable) rawTrackable));
        }
        // Previously tracked issues that are not detected anymore should be removed
        for (var previouslyTracked : matchingResult.getUnmatchedRights()) {
          trackedIssues.remove(previouslyTracked);
        }
      } else {
        reloadTrackingStateFromPersistentStorage(file, rawIssues);
      }
    }
  }

  private void reloadTrackingStateFromPersistentStorage(ISonarLintFile file, Collection<RawIssueTrackable> rawIssues) {
    var previousPersistedIssues = Objects.requireNonNull(store.read(file.getProjectRelativePath()));
    var trackedIssues = new ArrayList<TrackedIssue>();
    Collection<MatchableIssue> rawIssuesMatchable = rawIssues.stream().map(r -> r).collect(Collectors.toList());
    var matchingResult = IssueMatcher.matchIssues(rawIssuesMatchable, previousPersistedIssues);
    // Deal with matching issues
    for (var entry : matchingResult.getMatchedRaws().entrySet()) {
      var cached = (ProtobufMatchableIssueAdapter) entry.getValue();
      var raw = (RawIssueTrackable) entry.getKey();
      var tracked = TrackedIssue.fromPersistentCache(cached, raw);
      trackedIssues.add(tracked);
    }
    // New local issues compared to previous persisted issues
    for (var rawTrackable : matchingResult.getUnmatchedLefts()) {
      trackedIssues.add(TrackedIssue.asNew((RawIssueTrackable) rawTrackable));
    }
    trackedIssuesPerRelativePath.put(file.getProjectRelativePath(), trackedIssues);
  }

  public synchronized void trackWithServerIssues(Collection<ISonarLintIssuable> issuables, boolean shouldUpdateServerIssues,
    IProgressMonitor monitor) {
    var issuesDtos = new HashMap<Path, List<ClientTrackedFindingDto>>();

    for (var issuable : issuables) {
      if (issuable instanceof ISonarLintFile) {
        var relativePath = ((ISonarLintFile) issuable).getProjectRelativePath();
        var localIssuesTracked = trackedIssuesPerRelativePath.get(relativePath);
        issuesDtos.put(Paths.get(relativePath), localIssuesTracked.stream().map(ProjectIssueTracker::convertFromTrackable).collect(Collectors.toList()));
      }
    }

    try {
      var response = JobUtils.waitForFuture(monitor, SonarLintBackendService.get().trackWithServerIssues(project, issuesDtos, shouldUpdateServerIssues));
      response.getIssuesByIdeRelativePath()
        .forEach((idePath, serverTrackedIssues) -> project.find(idePath.toString())
          .ifPresent(slFile -> {
            // trackWithServerIssues is guaranteeing that the two collections have the same size and same order
            var localIssuesTracked = trackedIssuesPerRelativePath.get(slFile.getProjectRelativePath());
            var index = 0;
            for (var tracked : localIssuesTracked) {
              tracked.updateFromSlCoreMatching(serverTrackedIssues.get(index));
              index++;
            }
          }));
    } catch (InterruptedException e) {
      SonarLintLogger.get().debug("Interrupted!", e);
      Thread.currentThread().interrupt();
    } catch (ExecutionException e) {
      SonarLintLogger.get().error("Error while tracking with server issues", e);
    }
  }

  private static ClientTrackedFindingDto convertFromTrackable(TrackedIssue issue) {
    var textRange = issue.getIssueFromAnalysis().getTextRange();
    var textRangeWithHash = textRange != null
      ? new TextRangeWithHashDto(textRange.getStartLine(), textRange.getStartLineOffset(), textRange.getEndLine(), textRange.getEndLineOffset(), issue.getTextRangeHash())
      : null;
    var lineWithHash = textRange != null ? new LineWithHashDto(textRange.getStartLine(), issue.getLineHash()) : null;
    return new ClientTrackedFindingDto(null, issue.getServerIssueKey(),
      textRangeWithHash,
      lineWithHash, issue.getRuleKey(), issue.getMessage());
  }

  private void trackAllRawIssuesAsUnknownCreationDate(ISonarLintFile file, Collection<RawIssueTrackable> rawIssues) {
    var trackedIssues = rawIssues.stream().map(TrackedIssue::asUnknownCreationDate).collect(Collectors.toList());
    trackedIssuesPerRelativePath.put(file.getProjectRelativePath(), trackedIssues);
  }

  public Collection<TrackedIssue> getTracked(ISonarLintFile file) {
    return trackedIssuesPerRelativePath.getOrDefault(file.getProjectRelativePath(), List.of());
  }

  public void clear() {
    trackedIssuesPerRelativePath.clear();
  }

  /**
   * Flushes all cached entries to disk.
   * It does not clear the cache.
   */
  public synchronized void flushAll() {
    SonarLintLogger.get().debug("Persisting all issues");
    trackedIssuesPerRelativePath.forEach((path, trackables) -> {
      try {
        store.save(path, trackables);
      } catch (IOException e) {
        throw new IllegalStateException("Failed to flush cache", e);
      }
    });
  }

}
