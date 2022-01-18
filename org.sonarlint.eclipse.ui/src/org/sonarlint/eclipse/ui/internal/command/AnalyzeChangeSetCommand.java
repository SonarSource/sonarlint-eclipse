/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2022 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.command;

import java.util.Collection;
import java.util.Collections;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeChangedFilesJob;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.util.SelectionUtils;
import org.sonarlint.eclipse.ui.internal.views.issues.SonarLintReportView;

public class AnalyzeChangeSetCommand extends AbstractHandler {

  @Nullable
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    var selectedProjects = SelectionUtils.allSelectedProjects(event, true);

    if (selectedProjects.isEmpty()) {
      var editedFile = AnalyzeCommand.findEditedFile(event);
      if (editedFile != null) {
        selectedProjects = Collections.singleton(editedFile.getFile().getProject());
      }
    }

    if (!selectedProjects.isEmpty()) {
      triggerAnalysis(selectedProjects);
    }

    return null;
  }

  private static void triggerAnalysis(Collection<ISonarLintProject> selectedProjects) {
    var job = new AnalyzeChangedFilesJob(selectedProjects);
    String reportTitle;
    if (selectedProjects.size() == 1) {
      reportTitle = "Changed files reported by the SCM on project " + selectedProjects.iterator().next().getName();
    } else {
      reportTitle = "Changed files reported by the SCM on " + selectedProjects.size() + " projects";
    }
    registerJobListener(job, reportTitle);
    job.schedule();
  }

  static void registerJobListener(Job job, String reportTitle) {
    job.addJobChangeListener(new JobChangeAdapter() {
      @Override
      public void done(IJobChangeEvent event) {
        if (Status.OK_STATUS == event.getResult()) {
          Display.getDefault().asyncExec(() -> {
            // Display SonarLint report view after analysis is completed
            var iw = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            try {
              iw.getActivePage().showView(SonarLintReportView.ID, null, IWorkbenchPage.VIEW_ACTIVATE);
              SonarLintReportView.setReportTitle(reportTitle);
            } catch (PartInitException e) {
              SonarLintLogger.get().error("Unable to open SonarLint Report View", e);
            }
          });
        } else if (Status.CANCEL_STATUS == event.getResult()) {
          Display.getDefault().asyncExec(() -> SonarLintReportView.setReportTitle(null));
        } else {
          Display.getDefault().asyncExec(() -> {
            // Display SonarLint report view to show status message
            var iw = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            try {
              iw.getActivePage().showView(SonarLintReportView.ID, null, IWorkbenchPage.VIEW_ACTIVATE);
              SonarLintReportView.setReportTitle(event.getResult().getMessage());
            } catch (PartInitException e) {
              SonarLintLogger.get().error("Unable to open SonarLint Report View", e);
            }
          });
        }
      }
    });
  }

}
