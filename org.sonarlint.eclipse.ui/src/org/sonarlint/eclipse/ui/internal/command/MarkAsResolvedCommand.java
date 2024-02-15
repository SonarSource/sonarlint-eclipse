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
package org.sonarlint.eclipse.ui.internal.command;

import java.util.List;
import java.util.concurrent.ExecutionException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.backend.SonarLintBackendService;
import org.sonarlint.eclipse.core.internal.engine.connected.ResolvedBinding;
import org.sonarlint.eclipse.core.internal.jobs.MarkAsResolvedJob;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.utils.JobUtils;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.dialog.MarkAnticipatedIssueAsResolvedDialog;
import org.sonarlint.eclipse.ui.internal.dialog.MarkAsResolvedDialog;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.issue.CheckStatusChangePermittedParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.issue.CheckStatusChangePermittedResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.issue.ResolutionStatus;

/**
 *  Command invoked on issues matched from SonarQube / SonarCloud or on anticipated issues in connection with
 *  SonarQube 10.2+ to resolve an issue based on server configuration and user authentication. Not available on issues
 *  only found locally!
 */
public class MarkAsResolvedCommand extends AbstractResolvedCommand {
  static {
    TITLE = "Mark Issue as Resolved";
  }

  @Override
  protected void execute(IMarker marker, ISonarLintFile file, ISonarLintProject project, String issueKey, boolean isTaint) {
    var checkJob = new Job("Check user permissions for setting the issue resolution") {
      private CheckStatusChangePermittedResponse result;
      private ResolvedBinding resolvedBinding;

      @Override
      protected IStatus run(IProgressMonitor monitor) {
        var binding = getBinding(marker);
        if (binding.isPresent()) {
          resolvedBinding = binding.get();
          try {
            result = JobUtils.waitForFuture(monitor, SonarLintBackendService.get().getBackend().getIssueService()
              .checkStatusChangePermitted(
                new CheckStatusChangePermittedParams(resolvedBinding.getProjectBinding().getConnectionId(), issueKey)));
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

    JobUtils.scheduleAfterSuccess(checkJob, () -> afterCheckSuccessful(marker, project, file, issueKey, isTaint,
      checkJob.result, checkJob.resolvedBinding));
    checkJob.schedule();
  }

  private void afterCheckSuccessful(IMarker marker, ISonarLintProject project, ISonarLintFile file, String issueKey,
    Boolean isTaintVulnerability, CheckStatusChangePermittedResponse result, ResolvedBinding resolvedBinding) {
    var hostURL = resolvedBinding.getConnectionFacade().getHost();
    var isSonarCloud = resolvedBinding.getConnectionFacade().isSonarCloud();

    if (!result.isPermitted()) {
      currentWindow.getShell().getDisplay()
        .asyncExec(() -> MessageDialog.openError(currentWindow.getShell(),
          TITLE + " on " + (isSonarCloud ? "SonarCloud" : "SonarQube"),
          result.getNotPermittedReason()));
      return;
    }

    currentWindow.getShell().getDisplay().asyncExec(() -> {
      var dialog = createDialog(marker, currentWindow.getShell(), result.getAllowedStatuses(), hostURL, isSonarCloud);
      if (dialog.open() == Window.OK) {
        var newStatus = dialog.getFinalTransition();
        var comment = dialog.getFinalComment();

        var job = new MarkAsResolvedJob(project, file, issueKey, newStatus, StringUtils.trimToNull(comment), isTaintVulnerability);
        job.schedule();
      }
    });
  }

  /** Get the correct dialog (differing for server / anticipated issues) */
  protected MarkAsResolvedDialog createDialog(IMarker marker, Shell parentShell, List<ResolutionStatus> transitions,
    String hostURL, boolean isSonarCloud) {
    var serverIssue = marker.getAttribute(MarkerUtils.SONAR_MARKER_SERVER_ISSUE_KEY_ATTR, null);
    return serverIssue != null
      ? new MarkAsResolvedDialog(parentShell, transitions, hostURL, isSonarCloud)
        : new MarkAnticipatedIssueAsResolvedDialog(parentShell, transitions, hostURL);
  }
}
