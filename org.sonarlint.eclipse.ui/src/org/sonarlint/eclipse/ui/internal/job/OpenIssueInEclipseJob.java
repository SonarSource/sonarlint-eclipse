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
package org.sonarlint.eclipse.ui.internal.job;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.engine.connected.ResolvedBinding;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest.FileWithDocument;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectsJob;
import org.sonarlint.eclipse.core.internal.jobs.TaintIssuesMarkerUpdateJob;
import org.sonarlint.eclipse.core.internal.markers.MarkerMatcher;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.preferences.SonarLintPreferencePage;
import org.sonarlint.eclipse.ui.internal.util.MessageDialogUtils;
import org.sonarlint.eclipse.ui.internal.util.PlatformUtils;
import org.sonarlint.eclipse.ui.internal.views.RuleDescriptionWebView;
import org.sonarlint.eclipse.ui.internal.views.issues.OnTheFlyIssuesView;
import org.sonarlint.eclipse.ui.internal.views.issues.TaintVulnerabilitiesView;
import org.sonarlint.eclipse.ui.internal.views.locations.IssueLocationsView;
import org.sonarsource.sonarlint.core.rpc.protocol.client.issue.IssueDetailsDto;

/**
 *  "Open in IDE": After covering most of cases where we cannot match the issue locally, this tries to match with the
 *                 actual markers we have on the files.
 */
public class OpenIssueInEclipseJob extends AbstractOpenInEclipseJob {
  private final String name;
  private final IssueDetailsDto issueDetails;
  private final ResolvedBinding binding;
  private final boolean recreatedMarkersAlready;
  private final boolean askedForPreferenceChangeAlready;

  public OpenIssueInEclipseJob(OpenIssueContext context) {
    super(context.getName(), context.getProject(), false);

    this.name = context.getName();
    this.issueDetails = context.getIssueDetails();
    this.binding = context.getBinding();
    this.recreatedMarkersAlready = context.getRecreatedMarkersAlready();
    this.askedForPreferenceChangeAlready = context.getAskedForPreferenceChangeAlready();
  }

  @Override
  protected IStatus actualRun() throws CoreException {
    return issueDetails.isTaint() ? handleTaintIssue() : handleNormalIssue();
  }

  @Override
  protected String getIdeFilePath() {
    return issueDetails.getIdeFilePath().toString();
  }

  /** Handle normal issues: They can be present as On-the-fly / Report markers */
  private IStatus handleNormalIssue() throws CoreException {
    // Check with possible On-The-Fly markers of that file
    var markerOpt = MarkerMatcher.tryMatchIssueWithOnTheFlyMarker(issueDetails, file);
    if (markerOpt.isEmpty()) {
      // Check with possible Report markers of that file
      markerOpt = MarkerMatcher.tryMatchIssueWithReportMarker(issueDetails, file);
      if (markerOpt.isEmpty()) {
        if (!recreatedMarkersAlready) {
          // Run analysis on the specific file and re-run this job
          var job = new AnalyzeProjectsJob(Map.of(file.getProject(), List.of(new FileWithDocument(file, null))));
          addJobChangeListener(job);
          job.schedule();

          return Status.OK_STATUS;
        }

        // Maybe the issue could not be found due to the workspace preferences hiding some markers
        return handlePossibleHiddenIssue();
      }
    }

    // Update the UI and open the correct editor and views
    updateUI(markerOpt.get());

    return Status.OK_STATUS;
  }

  /** Handle Taint Vulnerabilities: They can only be present as Taint markers */
  private IStatus handleTaintIssue() throws CoreException {
    var markerOpt = MarkerMatcher.tryMatchIssueWithTaintMarker(issueDetails, file);
    if (markerOpt.isEmpty()) {
      if (!recreatedMarkersAlready) {
        // Sync Taint Vulnerabilities and re-run this job
        var job = new TaintIssuesMarkerUpdateJob(binding.getConnectionFacade(), project, List.of(file));
        addJobChangeListener(job);
        job.schedule();

        return Status.OK_STATUS;
      }

      // Maybe the issue could not be found due to the workspace preferences hiding some markers
      return handlePossibleHiddenIssue();
    }

    // Update the UI and open the correct editor and views
    updateUI(markerOpt.get());

    return Status.OK_STATUS;
  }

  /** When invoking an external job we want to hop onto its result to re-run this job once again */
  private void addJobChangeListener(Job job) {
    job.addJobChangeListener(new JobChangeAdapter() {
      @Override
      public void done(IJobChangeEvent event) {
        if (Status.OK_STATUS == event.getResult()) {
          // Because the analysis is now decoupled from the actual raising of the issues, we cannot rely on the
          // analysis job shutting down to then say "oh maybe now we can open the issue in Eclipse because the markers
          // should be there". We give it some time.
          // This is a corner case and only happens if the file of the issue opened in the IDE was never analyzed
          // before. Basically the waiting ("scheduling") is on-top of what it takes to end the analysis.
          new OpenIssueInEclipseJob(new OpenIssueContext(name, issueDetails, project, binding, file, true))
            .schedule(1000);
        } else {
          MessageDialogUtils.openInEclipseFailed("Fetching the issue to be displayed failed because the dependent "
            + "job did not finish successfully.");
        }
      }
    });
  }

  /** When the workspace preferences are not in our favor we want the consent from the user to change them */
  private IStatus handlePossibleHiddenIssue() {
    // When we already asked the user to change his workspace preferences, he agreed but did not change anything we
    // don't want to end up in a loop asking the user if could please change his preferences ^^
    if (askedForPreferenceChangeAlready) {
      MessageDialogUtils.openInEclipseFailed("The issue was not found locally. Maybe the issue was already "
        + "resolved or the resources has moved / was deleted.");
      return Status.CANCEL_STATUS;
    }

    // Ask the user if we are allowed to change the workspace preferences
    MessageDialogUtils.openInIdeQuestion("The issue might not be found due to the workspace preferences "
      + "on the display of SonarQube markers. Please change the preferences and 'Apply and Close' them to continue!",
      () -> {
        var preferences = PlatformUtils.showPreferenceDialog(SonarLintPreferencePage.ID);
        var page = (SonarLintPreferencePage) preferences.getSelectedPage();
        page.setOpenIssueInEclipseJobParams(
          new OpenIssueContext(name, issueDetails, project, binding, file, true, true));
        preferences.open();
      });

    return Status.OK_STATUS;
  }

  /** Update the UI based on the marker information */
  private void updateUI(IMarker marker) {
    Display.getDefault().asyncExec(() -> {
      // Open the editor at the position of the marker
      var editor = PlatformUtils.openEditor(marker);

      try {
        // Open either Taint Vulnerabilities or On The Fly view
        if (issueDetails.isTaint()) {
          PlatformUtils.showView(TaintVulnerabilitiesView.ID).setFocus();
        } else {
          PlatformUtils.showView(OnTheFlyIssuesView.ID).setFocus();
        }

        // Show Issue Locations view only when there are some flows
        if (!issueDetails.getFlows().isEmpty()) {
          var issueLocationsView = (IssueLocationsView) PlatformUtils.showView(IssueLocationsView.ID);
          issueLocationsView.markerSelected(Optional.of(marker));
          issueLocationsView.setFocus();
        }

        // Open Rule Description view
        var ruleDescriptionView = (RuleDescriptionWebView) PlatformUtils.showView(RuleDescriptionWebView.ID);
        ruleDescriptionView.setInput(marker);
        ruleDescriptionView.setFocus();
      } catch (PartInitException e) {
        SonarLintLogger.get().error("Open in IDE: An error occoured while opening the SonarQube views", e);
      } finally {
        if (editor != null) {
          editor.setFocus();
        }
      }
    });
  }

  public static class OpenIssueContext {
    @Nullable
    private ISonarLintFile file;

    private final String name;
    private final IssueDetailsDto issueDetails;
    private final ISonarLintProject project;
    private final ResolvedBinding binding;
    private final boolean recreatedMarkersAlready;
    private final boolean askedForPreferenceChangeAlready;

    public OpenIssueContext(String name, IssueDetailsDto params, ISonarLintProject project,
      ResolvedBinding binding) {
      this.name = name;
      this.issueDetails = params;
      this.project = project;
      this.binding = binding;
      this.recreatedMarkersAlready = false;
      this.askedForPreferenceChangeAlready = false;
    }

    public OpenIssueContext(String name, IssueDetailsDto params, ISonarLintProject project,
      ResolvedBinding binding, ISonarLintFile file, boolean recreatedMarkersAlready) {
      this.name = name;
      this.issueDetails = params;
      this.project = project;
      this.binding = binding;
      this.file = file;
      this.recreatedMarkersAlready = recreatedMarkersAlready;
      this.askedForPreferenceChangeAlready = false;
    }

    public OpenIssueContext(String name, IssueDetailsDto params, ISonarLintProject project,
      ResolvedBinding binding, ISonarLintFile file, boolean recreatedMarkersAlready,
      boolean askedForPreferenceChangeAlready) {
      this.name = name;
      this.issueDetails = params;
      this.project = project;
      this.binding = binding;
      this.file = file;
      this.recreatedMarkersAlready = recreatedMarkersAlready;
      this.askedForPreferenceChangeAlready = askedForPreferenceChangeAlready;
    }

    @Nullable
    public ISonarLintFile getFile() {
      return file;
    }

    public String getName() {
      return name;
    }

    public IssueDetailsDto getIssueDetails() {
      return issueDetails;
    }

    public ISonarLintProject getProject() {
      return project;
    }

    public ResolvedBinding getBinding() {
      return binding;
    }

    public boolean getRecreatedMarkersAlready() {
      return recreatedMarkersAlready;
    }

    public boolean getAskedForPreferenceChangeAlready() {
      return askedForPreferenceChangeAlready;
    }
  }
}
