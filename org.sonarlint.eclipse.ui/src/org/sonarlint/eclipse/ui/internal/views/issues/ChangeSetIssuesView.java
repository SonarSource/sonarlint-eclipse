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
package org.sonarlint.eclipse.ui.internal.views.issues;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.markers.MarkerSupportView;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeChangedFilesJob;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public class ChangeSetIssuesView extends MarkerSupportView {

  public static final String ID = SonarLintUiPlugin.PLUGIN_ID + ".views.issues.ChangeSetIssuesView";
  private static LocalDateTime lastRefresh;
  private static ChangeSetIssuesView instance;
  private Label label;
  private Composite bottom;
  private Button btn;

  public ChangeSetIssuesView() {
    super(SonarLintUiPlugin.PLUGIN_ID + ".views.issues.changeSetIssueMarkerGenerator");
    instance = this;
  }

  @Override
  public void createPartControl(Composite parent) {
    GridLayout layout = new GridLayout(1, false);
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.horizontalSpacing = 0;
    layout.verticalSpacing = 0;
    parent.setLayout(layout);
    Composite issuesTable = new Composite(parent, SWT.NONE);
    GridData issuesLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
    issuesTable.setLayoutData(issuesLayoutData);
    super.createPartControl(issuesTable);
    bottom = new Composite(parent, SWT.NONE);
    RowLayout bottomLayout = new RowLayout();
    bottomLayout.center = true;
    bottom.setLayout(bottomLayout);
    GridData bottomLayoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
    bottom.setLayoutData(bottomLayoutData);
    btn = new Button(bottom, SWT.PUSH);
    btn.setImage(SonarLintImages.RUN_IMG);
    btn.setText("Analyze changed files");
    btn.addListener(SWT.Selection, e -> triggerAnalysis(
      asList(ResourcesPlugin.getWorkspace().getRoot().getProjects()).stream()
        .filter(IProject::isAccessible)
        .collect(toList())));
    label = new Label(bottom, SWT.NONE);
    refreshText();
  }

  private void refreshText() {
    String msg = "Find issues on changed files based on the SCM";
    if (lastRefresh != null) {
      label.setText(msg + ". Report generated on " + DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(lastRefresh));
    } else {
      label.setText(msg);
    }
  }

  public static void setRefreshTime(LocalDateTime now) {
    ChangeSetIssuesView.lastRefresh = now;
    instance.refreshText();
    instance.bottom.requestLayout();
  }

  public static void triggerAnalysis(Collection<IProject> selectedProjects) {
    // Disable button
    ChangeSetIssuesView.instance.btn.setEnabled(false);
    AnalyzeChangedFilesJob job = new AnalyzeChangedFilesJob(selectedProjects);
    registerJobListener(job);
    job.schedule();
  }

  private static void registerJobListener(Job job) {
    job.addJobChangeListener(new JobChangeAdapter() {
      @Override
      public void done(IJobChangeEvent event) {
        Display.getDefault().asyncExec(() -> {
          // Enable button
          ChangeSetIssuesView.instance.btn.setEnabled(true);
          if (Status.OK_STATUS == event.getResult()) {
            // Display changeset issues view after analysis is completed
            IWorkbenchWindow iw = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            try {
              iw.getActivePage().showView(ChangeSetIssuesView.ID, null, IWorkbenchPage.VIEW_VISIBLE);
              ChangeSetIssuesView.setRefreshTime(LocalDateTime.now());
            } catch (PartInitException e) {
              SonarLintUiPlugin.getDefault().getLog().log(new Status(Status.ERROR, SonarLintUiPlugin.PLUGIN_ID, Status.OK, "Unable to open ChangeSet Issues View", e));
            }
          }
        });
      }
    });
    job.schedule();
  }

}
