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

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.backend.ConfigScopeSynchronizer;
import org.sonarlint.eclipse.core.internal.jobs.AnalysisReadyStatusCache;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarlint.eclipse.ui.internal.dialog.AwaitProjectConnectionReadyDialog;
import org.sonarlint.eclipse.ui.internal.util.MessageDialogUtils;

/**
 *  Every feature that opens something in the Eclipse IDE (e.g. issues, fix suggestions, ...) does it via a background
 *  job and this is its base class with shared logic. As much as possible should be shared, but the branch check is not
 *  necessary for every project anymore!
 */
public abstract class AbstractOpenInEclipseJob extends Job {
  @Nullable
  protected ISonarLintFile file;

  protected final ISonarLintProject project;
  private final boolean skipAnalysisReadyCheck;

  protected AbstractOpenInEclipseJob(String name, ISonarLintProject project, boolean skipAnalysisReadyCheck) {
    super(name);

    this.project = project;
    this.skipAnalysisReadyCheck = skipAnalysisReadyCheck;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    // 1) We have to await the analysis getting ready for this project. When also setting up the connection / binding
    // with this Open in IDE request it isn't the case. We want to display a nice dialog informing the user that they
    // have to wait for a few more moments.
    // -> When the check is not needed, the whole stuff gets skipped!
    if (!skipAnalysisReadyCheck
      && !AnalysisReadyStatusCache.getAnalysisReadiness(ConfigScopeSynchronizer.getConfigScopeId(project))) {
      // Show the console so the user will see the progress!
      SonarLintUiPlugin.getDefault().getSonarConsole().bringConsoleToFront();

      // We have to run the dialog in the UI thread, therefore we have to work with references here and synchronized
      // threads for the job to proceed correctly later.
      var statusRef = new AtomicReference<IStatus>();
      var cancelledByJob = new AtomicBoolean();
      statusRef.set(Status.OK_STATUS);
      cancelledByJob.set(false);
      Display.getDefault().syncExec(() -> {
        var dialog = new AwaitProjectConnectionReadyDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
        new AwaitProjectConnectionReadyJob(project, dialog, 0)
          .schedule(AwaitProjectConnectionReadyJob.SCHEDULE_TIMER_MS);
        dialog.open();
        if (dialog.cancelledByJob() || dialog.cancelledByUser()) {
          SonarLintLogger.get().debug("Requested action after setting up Connected Mode cancelled by "
            + (dialog.cancelledByUser() ? "the user." : "timeout of the background job."));
          statusRef.set(Status.CANCEL_STATUS);
        }
        cancelledByJob.set(dialog.cancelledByJob());
      });

      // We only want to show a new dialog when the connection was not yet ready in time and not when the user manually
      // cancelled it (e.g. because their priority changed, it took too long for them or whatever)
      var status = statusRef.get();
      if (Status.CANCEL_STATUS == status) {
        if (cancelledByJob.get()) {
          MessageDialogUtils.dialogCancelled("The previous dialog was closed either manually by you or the connection "
            + "was not ready in time. The reason for the latter could be a slow network connection.");
        }
        return status;
      }
    }

    // 2) Check if matching between remote and local path does work / branches match
    // INFO: When we re-run this job we don't have to do all the checks again!
    if (file == null) {
      var fileOpt = tryGetLocalFile();
      if (fileOpt.isEmpty()) {
        return Status.CANCEL_STATUS;
      }
      file = fileOpt.get();
    }

    try {
      // 3) Based on the implementation, actually run it and handle its failure accordingly
      actualRun();
    } catch (CoreException e) {
      var message = "An error occurred while trying to run the requested action.";
      MessageDialogUtils.openInEclipseFailed(message + " Please see the console for the full error log!");
      SonarLintLogger.get().error(message, e);
    }

    return Status.CANCEL_STATUS;
  }

  /**
   *  File check: We try to convert the server path to IDE path in order to find the correct file
   *
   *  This is implementation-specific as third-parties can implement "ISonarLintProject" it for their own plug-in, e.g.
   *  for COBOL-IDEs where the local file is not really in the FS! Just FYI as this is a well-known point to break on
   *  all the >> Open in Eclipse IDE << feature -.-
   */
  private Optional<ISonarLintFile> tryGetLocalFile() {
    // Check if file exists in project based on the server to IDE path matching
    var fileOpt = project.find(getIdeFilePath());
    if (fileOpt.isEmpty()) {
      MessageDialogUtils.fileNotFound("The required file cannot be found in the project '"
        + project.getName() + "'. Maybe it was already changed locally!");
      return Optional.empty();
    }

    return fileOpt;
  }

  /**
   *  The actual feature specific logic after everything is set up accordingly and the base checks are done.
   *
   *  Contract applies that "file" (ISonarLintFile) is not set to "null" when this method is invoked! So no check for
   *  "null" is necessary anymore in here!
   */
  abstract IStatus actualRun() throws CoreException;

  /** As we work per "ISonarLintFile" we want to get the IDE (project relative) path from a file on the server */
  abstract String getIdeFilePath();
}
