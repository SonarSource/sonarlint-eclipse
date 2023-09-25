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
package org.sonarlint.eclipse.core.internal.jobs;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration.EclipseProjectBinding;
import org.sonarlint.eclipse.core.internal.tracking.ProjectIssueTracker;
import org.sonarlint.eclipse.core.internal.tracking.RawIssueTrackable;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintIssuable;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.analysis.api.AnalysisResults;
import org.sonarsource.sonarlint.core.analysis.api.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedAnalysisConfiguration;
import org.sonarsource.sonarlint.core.serverconnection.ProjectBinding;

public class AnalyzeConnectedProjectJob extends AbstractAnalyzeProjectJob<ConnectedAnalysisConfiguration> {

  private final EclipseProjectBinding binding;
  private final ConnectedEngineFacade engineFacade;

  public AnalyzeConnectedProjectJob(AnalyzeProjectRequest request, EclipseProjectBinding binding, ConnectedEngineFacade engineFacade) {
    super(request);
    this.binding = binding;
    this.engineFacade = engineFacade;
  }

  @Override
  protected ConnectedAnalysisConfiguration prepareAnalysisConfig(Path projectBaseDir, List<ClientInputFile> inputFiles, Map<String, String> mergedExtraProps) {
    SonarLintLogger.get().debug("Connected mode (using configuration of '" + binding.projectKey() + "' in connection '" + binding.connectionId() + "')");
    return ConnectedAnalysisConfiguration.builder()
      .setProjectKey(binding.projectKey())
      .setBaseDir(projectBaseDir)
      .addInputFiles(inputFiles)
      .putAllExtraProperties(mergedExtraProps)
      .build();
  }

  @Override
  protected AnalysisResults runAnalysis(ConnectedAnalysisConfiguration analysisConfig, SonarLintIssueListener issueListener, IProgressMonitor monitor) {
    return engineFacade.runAnalysis(analysisConfig, issueListener, monitor);
  }

  @Override
  protected void trackIssues(Map<ISonarLintFile, IDocument> docPerFile, Map<ISonarLintIssuable, List<Issue>> rawIssuesPerResource, TriggerType triggerType,
    IProgressMonitor monitor) {
    super.trackIssues(docPerFile, rawIssuesPerResource, triggerType, monitor);
    if (triggerType.shouldMatchAsync()) {
      new ServerIssueTrackingAndMarkerUpdateJob(getProject(), binding, rawIssuesPerResource.keySet(), docPerFile, triggerType).schedule();
    }
  }

  @Override
  protected void trackFileIssues(ISonarLintFile file, List<RawIssueTrackable> trackables, ProjectIssueTracker issueTracker, TriggerType triggerType,
    int totalTrackedFiles,
    IProgressMonitor monitor) {
    super.trackFileIssues(file, trackables, issueTracker, triggerType, totalTrackedFiles, monitor);
    if (!triggerType.shouldMatchAsync()) {
      issueTracker.trackWithServerIssues(binding, List.of(file), triggerType.shouldUpdate(), monitor);
    }
  }

  private class ServerIssueTrackingAndMarkerUpdateJob extends Job {
    private final ProjectBinding projectBinding;
    private final Collection<ISonarLintIssuable> issuables;
    private final ISonarLintProject project;
    private final Map<ISonarLintFile, IDocument> docPerFile;
    private final TriggerType triggerType;

    private ServerIssueTrackingAndMarkerUpdateJob(ISonarLintProject project,
      ProjectBinding projectBinding, Collection<ISonarLintIssuable> issuables, Map<ISonarLintFile, IDocument> docPerFile,
      TriggerType triggerType) {
      super("Fetch server issues for " + project.getName());
      this.docPerFile = docPerFile;
      this.triggerType = triggerType;
      setPriority(DECORATE);
      this.project = project;
      this.projectBinding = projectBinding;
      this.issuables = issuables;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      var issueTracker = SonarLintCorePlugin.getOrCreateIssueTracker(project);
      issueTracker.trackWithServerIssues(projectBinding, issuables, triggerType.shouldUpdate(), monitor);
      if (monitor.isCanceled()) {
        return Status.CANCEL_STATUS;
      }
      var trackedIssuesPerFile = issuables.stream()
        .filter(ISonarLintFile.class::isInstance)
        .map(f -> (ISonarLintFile) f)
        .collect(Collectors.toMap(f -> f, issueTracker::getTracked));

      new AsyncServerMarkerUpdaterJob(project, trackedIssuesPerFile, docPerFile, triggerType).schedule();
      return Status.OK_STATUS;
    }

  }
}
