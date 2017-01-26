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
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeChangedFilesJob;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public class ChangeSetIssuesView extends MarkerViewWithBottomPanel {

  public static final String ID = SonarLintUiPlugin.PLUGIN_ID + ".views.issues.ChangeSetIssuesView";
  private static LocalDateTime lastRefresh;
  private static ChangeSetIssuesView instance;
  private Label label;
  private Button btnPrj;
  private Composite btnPrjWrapper;
  private Button btnAll;
  private Composite bottom;

  public ChangeSetIssuesView() {
    super(SonarLintUiPlugin.PLUGIN_ID + ".views.issues.changeSetIssueMarkerGenerator");
    instance = this;
  }

  @Override
  public void dispose() {
    instance = null;
  }

  @Override
  protected void populateBottomPanel(Composite bottom) {
    this.bottom = bottom;
    RowLayout bottomLayout = new RowLayout();
    bottomLayout.center = true;
    bottom.setLayout(bottomLayout);
    GridData bottomLayoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
    bottom.setLayoutData(bottomLayoutData);

    Label caption = new Label(bottom, SWT.NONE);
    caption.setText("Run the analysis and find issues on the files in the SCM change set:");

    btnPrjWrapper = new Composite(bottom, SWT.NONE);
    // default values so it doesn't grab excess space
    btnPrjWrapper.setLayoutData(new RowData());
    btnPrjWrapper.setLayout(new FillLayout());
    btnPrj = new Button(btnPrjWrapper, SWT.PUSH);
    btnPrj.setText("Current project");
    refreshBtnState();

    btnPrj.addListener(SWT.Selection, e -> {
      IFile editedFile = SonarLintUiPlugin.findCurrentEditedFile();
      if (editedFile != null) {
        triggerAnalysis(asList(editedFile.getProject()));
      }
    });

    btnAll = new Button(bottom, SWT.PUSH);
    btnAll.setText("All projects");
    btnAll.setToolTipText("Analyze all changed files in all projects");
    btnAll.addListener(SWT.Selection, e -> triggerAnalysis(
      asList(ResourcesPlugin.getWorkspace().getRoot().getProjects()).stream()
        .filter(IProject::isAccessible)
        .collect(toList())));
    label = new Label(bottom, SWT.NONE);
    refreshText();
  }

  private void refreshBtnState() {
    IFile editedFile = SonarLintUiPlugin.findCurrentEditedFile();
    if (editedFile == null) {
      btnPrjWrapper.setToolTipText("No editor opened");
      btnPrj.setEnabled(false);
    } else {
      IProject project = editedFile.getProject();
      RepositoryProvider provider = RepositoryProvider.getProvider(project);
      if (provider == null) {
        btnPrjWrapper.setToolTipText("No SCM for project '" + project.getName() + "'");
        btnPrj.setEnabled(false);
      } else if (provider.getSubscriber() == null) {
        btnPrjWrapper.setToolTipText("Unsupported SCM for project '" + project.getName() + "'");
        btnPrj.setEnabled(false);
      } else {
        btnPrjWrapper.setToolTipText(project.getName());
        btnPrj.setEnabled(true);
      }
    }
  }

  private void refreshText() {
    if (lastRefresh != null) {
      label.setText("Report generated on " + DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(lastRefresh));
    } else {
      label.setText("");
    }
  }

  public static void notifyEditorChanged() {
    if (ChangeSetIssuesView.instance != null) {
      instance.refreshBtnState();
      instance.requestLayout();
    }
  }

  public static void setRefreshTime(LocalDateTime now) {
    ChangeSetIssuesView.lastRefresh = now;
    if (ChangeSetIssuesView.instance != null) {
      instance.refreshText();
      instance.requestLayout();
    }
  }

  private void requestLayout() {
    // TODO replace by requestLayout() when supporting only Eclipse 4.6+
    bottom.getShell().layout(new Control[] {instance.bottom}, SWT.DEFER);
  }

  public static void triggerAnalysis(Collection<IProject> selectedProjects) {
    // Disable button if view is visible
    if (ChangeSetIssuesView.instance != null) {
      ChangeSetIssuesView.instance.btnAll.setEnabled(false);
      ChangeSetIssuesView.instance.btnPrj.setEnabled(false);
    }
    AnalyzeChangedFilesJob job = new AnalyzeChangedFilesJob(selectedProjects);
    registerJobListener(job);
    job.schedule();
  }

  private static void registerJobListener(Job job) {
    job.addJobChangeListener(new JobChangeAdapter() {
      @Override
      public void done(IJobChangeEvent event) {
        Display.getDefault().asyncExec(() -> {
          // Enable button if view is visible
          if (ChangeSetIssuesView.instance != null) {
            ChangeSetIssuesView.instance.btnAll.setEnabled(true);
            ChangeSetIssuesView.instance.refreshBtnState();
          }
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
  }

}
