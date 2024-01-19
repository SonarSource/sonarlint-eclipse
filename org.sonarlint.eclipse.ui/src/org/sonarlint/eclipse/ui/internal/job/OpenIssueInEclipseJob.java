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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.documentation.SonarLintDocumentation;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.engine.connected.ResolvedBinding;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest.FileWithDocument;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectsJob;
import org.sonarlint.eclipse.core.internal.jobs.TaintIssuesUpdateAfterSyncJob;
import org.sonarlint.eclipse.core.internal.markers.MarkerMatcher;
import org.sonarlint.eclipse.core.internal.vcs.VcsService;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.preferences.SonarLintPreferencePage;
import org.sonarlint.eclipse.ui.internal.util.BrowserUtils;
import org.sonarlint.eclipse.ui.internal.util.MessageDialogUtils;
import org.sonarlint.eclipse.ui.internal.util.PlatformUtils;
import org.sonarlint.eclipse.ui.internal.views.RuleDescriptionWebView;
import org.sonarlint.eclipse.ui.internal.views.issues.OnTheFlyIssuesView;
import org.sonarlint.eclipse.ui.internal.views.issues.TaintVulnerabilitiesView;
import org.sonarlint.eclipse.ui.internal.views.locations.IssueLocationsView;
import org.sonarsource.sonarlint.core.clientapi.client.issue.ShowIssueParams;

/**
 *  "Open in IDE": After covering most of cases where we cannot match the issue locally, this tries to match with the
 *                 actual markers we have on the files.
 */
public class OpenIssueInEclipseJob extends Job {
  @Nullable
  private ISonarLintFile file;

  private final String name;
  private final ShowIssueParams params;
  private final ISonarLintProject project;
  private final ResolvedBinding binding;
  private final boolean recreatedMarkersAlready;
  private final boolean askedForPreferenceChangeAlready;

  public OpenIssueInEclipseJob(OpenIssueContext context) {
    super(context.getName());

    this.name = context.getName();
    this.params = context.getParams();
    this.project = context.getProject();
    this.binding = context.getBinding();
    this.recreatedMarkersAlready = context.getRecreatedMarkersAlready();
    this.askedForPreferenceChangeAlready = context.getAskedForPreferenceChangeAlready();
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    // 1) Check if matching between remote and local path does work / branches match
    // INFO: When we re-run this job we don't have to do all the checks again!
    if (file == null) {
      var fileOpt = tryGetLocalFile();
      if (fileOpt.isEmpty()) {
        return Status.CANCEL_STATUS;
      }
      file = fileOpt.get();

      if (!tryMatchBranches()) {
        return Status.CANCEL_STATUS;
      }
    }

    try {
      // 2) Handle normal issues / Taint Vulnerabilities differently
      return params.isTaint() ? handleTaintIssue() : handleNormalIssue();
    } catch (CoreException e) {
      var message = "An error occured while trying to match the issue locally.";

      MessageDialogUtils.openInIdeError(message + " Please see the console for the full error log!");
      SonarLintLogger.get().error(message, e);
    }

    return Status.CANCEL_STATUS;
  }

  /** File check: We try to convert the server path to IDE path in order to find the correct file */
  private Optional<ISonarLintFile> tryGetLocalFile() {
    // Check if the server file path can be matched to a local file path
    var filePathOpt = binding.getProjectBinding().serverPathToIdePath(params.getServerRelativeFilePath());
    if (filePathOpt.isEmpty()) {
      MessageDialogUtils.openInIdeError("The file containing the issue cannot be located in the project '"
        + project.getName() + "'. Maybe it was already changed locally!");
      return Optional.empty();
    }

    // Check if file exists in project based on the server to IDE path matching
    var fileOpt = project.find(filePathOpt.get());
    if (fileOpt.isEmpty()) {
      MessageDialogUtils.openInIdeError("The file containing the issue cannot be found in the project '"
        + project.getName() + "'. Maybe it was already changed locally!");
      return Optional.empty();
    }

    return fileOpt;
  }

  /** Branch check: Local and remote information should match (if no local branch found, at least try your best) */
  private boolean tryMatchBranches() {
    var branch = params.getBranch();

    var localBranch = VcsService.getServerBranch(project);
    if (localBranch.isEmpty()) {
      // This error message may be misleading to COBOL / ABAP developers but that is okay for now :>
      MessageDialogUtils.openInIdeInformation("The local branch of the project '" + project.getName()
        + "' could not be determined. SonarLint now can only try to find the matching local issue!");
    } else if (!branch.equals(localBranch.get())) {
      MessageDialogUtils.openInIdeError("The local branch '" + localBranch.get() + "' of the project '"
        + project.getName() + "' does not match the remote branch '" + branch + "'. "
        + "Please checkout the correct branch and invoke 'Open in IDE' once again!");
      BrowserUtils.openExternalBrowser(SonarLintDocumentation.BRANCH_AWARENESS, Display.getDefault());
      return false;
    }

    return true;
  }

  /** Handle normal issues: They can be present as On-the-fly / Report markers */
  private IStatus handleNormalIssue() throws CoreException {
    // Check with possible On-The-Fly markers of that file
    var markerOpt = MarkerMatcher.tryMatchIssueWithOnTheFlyMarker(params, file);
    if (markerOpt.isEmpty()) {
      // Check with possible Report markers of that file
      markerOpt = MarkerMatcher.tryMatchIssueWithReportMarker(params, file);
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
    var markerOpt = MarkerMatcher.tryMatchIssueWithTaintMarker(params, file);
    if (markerOpt.isEmpty()) {
      if (!recreatedMarkersAlready) {
        // Sync Taint Vulnerabilities and re-run this job
        var job = new TaintIssuesUpdateAfterSyncJob((ConnectedEngineFacade) binding.getEngineFacade(), project,
          List.of(file));
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
          new OpenIssueInEclipseJob(new OpenIssueContext(name, params, project, binding, file, true))
            .schedule();
        } else {
          MessageDialogUtils.openInIdeError("Fetching the issue to be displayed failed because the dependent "
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
      MessageDialogUtils.openInIdeError("The issue was not found locally. Maybe the issue was already "
        + "resolved or the resources has moved / was deleted.");
      return Status.CANCEL_STATUS;
    }

    // Ask the user if we are allowed to change the workspace preferences
    MessageDialogUtils.openInIdeQuestion("The issue might not be found due to the workspace preferences "
      + "on the display of SonarLint markers. Please change the preferences and 'Apply and Close' them to continue!",
      () -> {
        var preferences = PlatformUtils.showPreferenceDialog(SonarLintPreferencePage.ID);
        var page = (SonarLintPreferencePage) preferences.getSelectedPage();
        page.setOpenIssueInEclipseJobParams(
          new OpenIssueContext(name, params, project, binding, file, true, true));
        preferences.open();
      });

    return Status.OK_STATUS;
  }

  /** Update the UI based on the marker information */
  private void updateUI(IMarker marker) {
    Display.getDefault().asyncExec(() -> {
      // Open the editor at the position of the marker
      PlatformUtils.openEditor(marker);

      try {
        // Open either Taint Vulnerabilities or On The Fly view
        if (params.isTaint()) {
          PlatformUtils.showView(TaintVulnerabilitiesView.ID).setFocus();
        } else {
          PlatformUtils.showView(OnTheFlyIssuesView.ID).setFocus();
        }

        // Show Issue Locations view only when there are some flows
        if (!params.getFlows().isEmpty()) {
          var issueLocationsView = (IssueLocationsView) PlatformUtils.showView(IssueLocationsView.ID);
          issueLocationsView.markerSelected(Optional.of(marker));
          issueLocationsView.setFocus();
        }

        // Open Rule Description view
        var ruleDescriptionView = (RuleDescriptionWebView) PlatformUtils.showView(RuleDescriptionWebView.ID);
        ruleDescriptionView.setInput(marker);
        ruleDescriptionView.setFocus();
      } catch (PartInitException e) {
        SonarLintLogger.get().error("Open in IDE: An error occoured while opening the SonarLint views", e);
      }
    });
  }

  public static class OpenIssueContext {
    @Nullable
    private ISonarLintFile file;

    private final String name;
    private final ShowIssueParams params;
    private final ISonarLintProject project;
    private final ResolvedBinding binding;
    private final boolean recreatedMarkersAlready;
    private final boolean askedForPreferenceChangeAlready;

    public OpenIssueContext(String name, ShowIssueParams params, ISonarLintProject project,
      ResolvedBinding binding) {
      this.name = name;
      this.params = params;
      this.project = project;
      this.binding = binding;
      this.recreatedMarkersAlready = false;
      this.askedForPreferenceChangeAlready = false;
    }

    public OpenIssueContext(String name, ShowIssueParams params, ISonarLintProject project,
      ResolvedBinding binding, ISonarLintFile file, boolean recreatedMarkersAlready) {
      this.name = name;
      this.params = params;
      this.project = project;
      this.binding = binding;
      this.file = file;
      this.recreatedMarkersAlready = recreatedMarkersAlready;
      this.askedForPreferenceChangeAlready = false;
    }

    public OpenIssueContext(String name, ShowIssueParams params, ISonarLintProject project,
      ResolvedBinding binding, ISonarLintFile file, boolean recreatedMarkersAlready,
      boolean askedForPreferenceChangeAlready) {
      this.name = name;
      this.params = params;
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

    public ShowIssueParams getParams() {
      return params;
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
