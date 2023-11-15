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
package org.sonarlint.eclipse.ui.internal.command;

import java.util.concurrent.ExecutionException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.backend.SonarLintBackendService;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.engine.connected.ResolvedBinding;
import org.sonarlint.eclipse.core.internal.jobs.ReOpenResolvedJob;
import org.sonarlint.eclipse.core.internal.utils.JobUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.clientapi.backend.issue.ReopenIssueResponse;

/**
 *  Command invoked on issues already resolved (either as anticipated issue or issue already known to the server).
 */
public class ReOpenResolvedCommand extends AbstractResolvedCommand {
  static {
    TITLE = "Re-Opening resolved Issue";
  }
  
  @Override
  protected void execute(IMarker marker, ISonarLintFile file, ISonarLintProject project, String issueKey,
    boolean isTaint) {
    var checkJob = new Job("Check user permissions for setting the issue resolution") {
      private ReopenIssueResponse result;
      private ResolvedBinding resolvedBinding;

      @Override
      protected IStatus run(IProgressMonitor monitor) {
        var binding = getBinding(marker);
        if (binding.isPresent()) {
          resolvedBinding = binding.get();
          try {
            result = JobUtils.waitForFuture(monitor,
              SonarLintBackendService.get().reopenIssue(project, issueKey, isTaint));
            return Status.OK_STATUS;
          } catch (ExecutionException e) {
            return new Status(IStatus.ERROR, SonarLintCorePlugin.PLUGIN_ID,
              e.getCause() != null ? e.getCause().getMessage() : e.getMessage(), e);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Status(IStatus.CANCEL, SonarLintCorePlugin.PLUGIN_ID, e.getMessage(), e);
          }
        } else {
          return Status.error("Project is not bound anymore");
        }
      }
    };

    JobUtils.scheduleAfterSuccess(checkJob, () -> afterCheckSuccessful(project, file, isTaint, checkJob.result,
      checkJob.resolvedBinding));
    checkJob.schedule();
  }
  
  private void afterCheckSuccessful(ISonarLintProject project, ISonarLintFile file, Boolean isTaintVulnerability,
    ReopenIssueResponse result, ResolvedBinding resolvedBinding) {
    var isSonarCloud = resolvedBinding.getEngineFacade().isSonarCloud();

    if (!result.isIssueReopened()) {
      currentWindow.getShell().getDisplay()
        .asyncExec(() -> MessageDialog.openError(currentWindow.getShell(),
          TITLE + " on " + (isSonarCloud ? "SonarCloud" : "SonarQube"),
          "Could not re-open the resolved Issue!"));
      return;
    }

    currentWindow.getShell().getDisplay().asyncExec(() -> {
      var job = new ReOpenResolvedJob(project, (ConnectedEngineFacade) resolvedBinding.getEngineFacade(), file,
        isTaintVulnerability);
      job.schedule();
    });
  }
}
