/*
 * SonarLint for Eclipse
 * Copyright (C) SonarSource Sàrl
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
package org.sonarlint.eclipse.core.internal.jobs;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.rpc.protocol.client.issue.RaisedIssueDto;

public class IssuesMarkerUpdateJob extends AbstractSonarJob {

  public static final IssuesMarkerUpdateJob INSTANCE = new IssuesMarkerUpdateJob();

  private final Queue<Request> workQueue = new ConcurrentLinkedQueue<>();

  private IssuesMarkerUpdateJob() {
    super("Update issues markers for projects");
  }

  public void add(ISonarLintProject project, Map<URI, List<RaisedIssueDto>> issuesByFileUri, boolean issuesAreOnTheFly) {
    workQueue.add(new Request(project, issuesByFileUri, issuesAreOnTheFly));
    schedule();
  }

  @Override
  protected IStatus doRun(IProgressMonitor monitor) throws CoreException {
    var requests = new ArrayList<Request>();
    Request request;
    while ((request = workQueue.poll()) != null) {
      requests.add(request);
    }

    if (requests.isEmpty()) {
      return Status.OK_STATUS;
    }

    var status = new AtomicReference<>(Status.OK_STATUS);
    var toNotify = new HashSet<ISonarLintProject>();
    // Requests that could not be processed (cancellation) are re-queued so their markers are not lost.
    var remaining = new ArrayList<Request>();

    ResourcesPlugin.getWorkspace().run(m -> {
      for (var i = 0; i < requests.size(); i++) {
        var req = requests.get(i);
        if (monitor.isCanceled()) {
          status.set(Status.CANCEL_STATUS);
          remaining.addAll(requests.subList(i, requests.size()));
          return;
        }
        try {
          updateProject(req);
          toNotify.add(req.project);
        } catch (RuntimeException e) {
          // A failure for one project must not prevent the other projects' markers from being updated.
          SonarLintLogger.get().error("Failed to update issue markers for project '" + req.project.getName() + "'", e);
        }
      }
    }, monitor);

    if (!toNotify.isEmpty()) {
      SonarLintCorePlugin.getAnalysisListenerManager().notifyListeners(() -> toNotify);
    }

    if (!remaining.isEmpty()) {
      workQueue.addAll(remaining);
    }

    // New requests may have been queued while this job was running, or requests were re-queued after cancellation.
    if (!workQueue.isEmpty()) {
      schedule();
    }

    return status.get();
  }

  private static void updateProject(Request req) {
    var countAllIssues = req.issuesByFileUri.values().stream().mapToInt(List::size).sum();
    SonarLintLogger.get().info("Found " + countAllIssues + " issue(s) on project '" + req.project.getName() + "'");

    var issuesIncludingResolved = SonarLintGlobalConfiguration.issuesIncludingResolved();
    var issuesOnlyNewCode = SonarLintGlobalConfiguration.issuesOnlyNewCode();
    var viableForStatusChange = SonarLintUtils.checkProjectSupportsAnticipatedStatusChange(req.project);

    for (var entry : req.issuesByFileUri.entrySet()) {
      var slFile = SonarLintUtils.findFileFromUri(entry.getKey());
      if (slFile != null) {
        SonarLintMarkerUpdater.createOrUpdateMarkers(slFile, entry.getValue(), req.issuesAreOnTheFly,
          issuesIncludingResolved, issuesOnlyNewCode, viableForStatusChange);
      }
    }
  }

  private static final class Request {
    private final ISonarLintProject project;
    private final Map<URI, List<RaisedIssueDto>> issuesByFileUri;
    private final boolean issuesAreOnTheFly;

    private Request(ISonarLintProject project, Map<URI, List<RaisedIssueDto>> issuesByFileUri, boolean issuesAreOnTheFly) {
      this.project = project;
      this.issuesByFileUri = issuesByFileUri;
      this.issuesAreOnTheFly = issuesAreOnTheFly;
    }
  }
}
