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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.sonarsource.sonarlint.core.clientapi.backend.tracking.ClientTrackedIssueDto;
import org.sonarsource.sonarlint.core.clientapi.backend.tracking.LineWithHashDto;
import org.sonarsource.sonarlint.core.clientapi.backend.tracking.LocalOnlyIssueDto;
import org.sonarsource.sonarlint.core.clientapi.backend.tracking.ServerMatchedIssueDto;
import org.sonarsource.sonarlint.core.clientapi.backend.tracking.TextRangeWithHashDto;
import org.sonarsource.sonarlint.core.serverconnection.IssueStorePaths;
import org.sonarsource.sonarlint.core.serverconnection.ProjectBinding;

/**
 * Keep track of all issues for a given (Eclipse) project
 */
public class ProjectIssueTracker {

  private final PersistentLocalIssueStore store;
  private final Map<String, Collection<TrackedIssue>> trackedIssuesPerRelativePath = new HashMap<>();
  private final ISonarLintProject project;

  public ProjectIssueTracker(ISonarLintProject project) {
    this.project = project;
    var storeBasePath = StoragePathManager.getIssuesDir(project);
    this.store = new PersistentLocalIssueStore(storeBasePath, project);
  }

  private boolean isFirstAnalysis(String file) {
    return !trackedIssuesPerRelativePath.containsKey(file) && !store.contains(file);
  }

  /**
   * Process the result of a fresh analysis.
   */
  public synchronized void processRawIssues(ISonarLintFile file, Collection<RawIssueTrackable> rawIssues) {
    if (isFirstAnalysis(file.getProjectRelativePath())) {
      trackAllRawIssuesAsUnknownCreationDate(file, rawIssues);
    } else {
      Collection<MatchableIssue> rawIssuesMatchable = rawIssues.stream().map(r -> r).collect(Collectors.toList());
      if (trackedIssuesPerRelativePath.containsKey(file.getProjectRelativePath())) {
        var previousIssues = trackedIssuesPerRelativePath.get(file.getProjectRelativePath());
        if (previousIssues == null || previousIssues.isEmpty()) {
          trackAllRawIssuesAsNew(file, rawIssues);
        } else {
          var matchingResult = IssueMatcher.matchIssues(rawIssuesMatchable, previousIssues);
          // Deal with matching issues
          for (var entry : matchingResult.getMatchedRaws().entrySet()) {
            var previous = (TrackedIssue) entry.getValue();
            var raw = (RawIssueTrackable) entry.getKey();
            previous.updateFromFreshAnalysis(raw);
          }
          // New local issues compared to previous cached issues
          for (var rawTrackable : matchingResult.getUnmatchedLefts()) {
            previousIssues.add(TrackedIssue.asNew((RawIssueTrackable) rawTrackable));
          }
          for (var previouslyTracked : matchingResult.getUnmatchedRights()) {
            previousIssues.remove(previouslyTracked);
          }
        }
      } else {
        var previousPersistedIssues = store.read(file.getProjectRelativePath());
        if (previousPersistedIssues == null) {
          trackAllRawIssuesAsNew(file, rawIssues);
        } else {
          var trackedIssues = new ArrayList<TrackedIssue>();
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
      }
    }
  }

  public synchronized void trackWithServerIssues(ProjectBinding projectBinding, Collection<ISonarLintIssuable> issuables, boolean shouldUpdateServerIssues,
    IProgressMonitor monitor) {
    var issuesDtos = new HashMap<String, List<ClientTrackedIssueDto>>();

    for (var issuable : issuables) {
      if (issuable instanceof ISonarLintFile) {
        var file = ((ISonarLintFile) issuable);

        var serverRelativePath = IssueStorePaths.idePathToServerPath(projectBinding, file.getProjectRelativePath());
        var localIssuesTracked = trackedIssuesPerRelativePath.get(file.getProjectRelativePath());
        issuesDtos.put(serverRelativePath, localIssuesTracked.stream().map(ProjectIssueTracker::convertFromTrackable).collect(Collectors.toList()));
      }
    }

    try {

      var response = JobUtils.waitForFuture(monitor, SonarLintBackendService.get().trackWithServerIssues(project, issuesDtos, shouldUpdateServerIssues));

      response.getIssuesByServerRelativePath().forEach((serverPath, serverTrackedIssues) -> {
        projectBinding.serverPathToIdePath(serverPath).flatMap(project::find).ifPresent(slFile -> {
          serverTrackedIssues.forEach(resultIssue -> {
            var resultUuid = resultIssue.map(ServerMatchedIssueDto::getId, LocalOnlyIssueDto::getId);
            var trackedIssue = trackedIssuesPerRelativePath.get(slFile.getProjectRelativePath()).stream().filter(i -> i.getUuid().equals(resultUuid)).findFirst();
            if (trackedIssue.isEmpty()) {
              // Possibly removed in the meantime?
              return;
            }
            trackedIssue.get().updateFromSlCoreMatching(resultIssue);
          });

        });
      });
    } catch (InterruptedException e) {
      SonarLintLogger.get().debug("Interrupted!", e);
      Thread.currentThread().interrupt();
    } catch (ExecutionException e) {
      SonarLintLogger.get().error("Error while tracking with server issues", e);
    }
  }

  private static ClientTrackedIssueDto convertFromTrackable(TrackedIssue issue) {
    var textRange = issue.getIssueFromAnalysis().getTextRange();
    var textRangeWithHash = textRange != null
      ? new TextRangeWithHashDto(textRange.getStartLine(), textRange.getStartLineOffset(), textRange.getEndLine(), textRange.getEndLineOffset(), issue.getTextRangeHash())
      : null;
    var lineWithHash = textRange != null ? new LineWithHashDto(textRange.getStartLine(), issue.getLineHash()) : null;
    return new ClientTrackedIssueDto(issue.getUuid(), issue.getServerIssueKey(),
      textRangeWithHash,
      lineWithHash, issue.getRuleKey(), issue.getMessage());
  }

  private void trackAllRawIssuesAsUnknownCreationDate(ISonarLintFile file, Collection<RawIssueTrackable> rawIssues) {
    var trackedIssues = rawIssues.stream().map(TrackedIssue::asUnknownCreationDate).collect(Collectors.toList());
    trackedIssuesPerRelativePath.put(file.getProjectRelativePath(), trackedIssues);
  }

  private void trackAllRawIssuesAsNew(ISonarLintFile file, Collection<RawIssueTrackable> rawIssues) {
    var trackedIssues = rawIssues.stream().map(TrackedIssue::asNew).collect(Collectors.toList());
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
