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
package org.sonarlint.eclipse.core.internal.jobs;

import java.util.Set;
import java.util.UUID;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.backend.ConfigScopeSynchronizer;
import org.sonarlint.eclipse.core.internal.backend.RunningAnalysesTracker;
import org.sonarlint.eclipse.core.internal.event.AnalysisEvent;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

public class IssuesMarkerUpdateJob extends AbstractSonarJob {
  private final ISonarLintProject project;
  private final String configScopeId;
  private final UUID analysisId;

  public IssuesMarkerUpdateJob(ISonarLintProject project, UUID analysisId) {
    super("Update issues markers for");
    this.project = project;
    this.analysisId = analysisId;
    this.configScopeId = ConfigScopeSynchronizer.getConfigScopeId(project);
  }

  @Override
  protected IStatus doRun(IProgressMonitor monitor) throws CoreException {
    var currentAnalysis = RunningAnalysesTracker.get().getById(analysisId);
    if (currentAnalysis == null) {
      return Status.CANCEL_STATUS;
    }

    SonarLintLogger.get().info("Found " + currentAnalysis.getIssueCount(configScopeId) + " issue(s) on project '"
      + project.getName() + "'");

    // To access the preference service only once and not per issue
    var issueFilterPreference = SonarLintGlobalConfiguration.getIssueFilter();

    // To access the preference service only once and not per issue
    var issuePeriodPreference = SonarLintGlobalConfiguration.getIssuePeriod();

    // If the project connection offers changing the status on anticipated issues (SonarQube 10.2+) we can enable the
    // context menu option on the markers.
    var viableForStatusChange = SonarLintUtils.checkProjectSupportsAnticipatedStatusChange(project);

    for (var entry : currentAnalysis.getIssuesByFileUri(configScopeId).entrySet()) {
      var slFile = SonarLintUtils.findFileFromUri(entry.getKey());
      if (slFile != null) {
        SonarLintMarkerUpdater.createOrUpdateMarkers(slFile, entry.getValue(), currentAnalysis.getTriggerType(),
          issuePeriodPreference, issueFilterPreference, viableForStatusChange);
      }
    }

    RunningAnalysesTracker.get().finish(currentAnalysis);

    SonarLintCorePlugin.getAnalysisListenerManager().notifyListeners(new AnalysisEvent() {
      @Override
      public Set<ISonarLintProject> getProjects() {
        return Set.of(project);
      }
    });

    return Status.OK_STATUS;
  }
}
