/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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
import java.util.List;
import java.util.Map;
import java.util.Set;
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
  private final ISonarLintProject project;
  private final Map<URI, List<RaisedIssueDto>> issuesByFileUri;
  private final boolean issuesAreOnTheFly;

  public IssuesMarkerUpdateJob(ISonarLintProject project, Map<URI, List<RaisedIssueDto>> issuesByFileUri, boolean issuesAreOnTheFly) {
    super("Update issues markers for project " + project.getName());
    this.project = project;
    this.issuesByFileUri = issuesByFileUri;
    this.issuesAreOnTheFly = issuesAreOnTheFly;
  }

  @Override
  protected IStatus doRun(IProgressMonitor monitor) throws CoreException {
    SonarLintLogger.get().info("Found " + countAllIssues() + " issue(s) on project '"
      + project.getName() + "'");

    // To access the preference service only once and not per issue
    var issuesIncludingResolved = SonarLintGlobalConfiguration.issuesIncludingResolved();
    var issuesOnlyNewCode = SonarLintGlobalConfiguration.issuesOnlyNewCode();

    // If the project connection offers changing the status on anticipated issues (SonarQube 10.2+) we can enable the
    // context menu option on the markers.
    var viableForStatusChange = SonarLintUtils.checkProjectSupportsAnticipatedStatusChange(project);

    ResourcesPlugin.getWorkspace().run(m -> {
      for (var entry : issuesByFileUri.entrySet()) {
        var slFile = SonarLintUtils.findFileFromUri(entry.getKey());
        if (slFile != null) {
          SonarLintMarkerUpdater.createOrUpdateMarkers(slFile, entry.getValue(), issuesAreOnTheFly,
            issuesIncludingResolved, issuesOnlyNewCode, viableForStatusChange);
        }
      }

      SonarLintCorePlugin.getAnalysisListenerManager().notifyListeners(() -> Set.of(project));
    }, monitor);

    return Status.OK_STATUS;
  }

  private int countAllIssues() {
    return issuesByFileUri.values().stream().mapToInt(List::size).sum();
  }
}
