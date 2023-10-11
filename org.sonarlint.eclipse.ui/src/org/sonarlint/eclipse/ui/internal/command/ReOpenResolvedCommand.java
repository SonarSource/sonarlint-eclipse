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
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.backend.SonarLintBackendService;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.engine.connected.ResolvedBinding;
import org.sonarlint.eclipse.core.internal.jobs.ReOpenResolvedJob;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.utils.JobUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.clientapi.backend.issue.ReopenIssueParams;
import org.sonarsource.sonarlint.core.clientapi.backend.issue.ReopenIssueResponse;

/**
 *  Command invoked on issues already resolved (either as anticipated issue or issue already known to the server).
 */
public class ReOpenResolvedCommand extends AbstractResolvedCommand {
  @Override
  protected void execute(IMarker selectedMarker, IWorkbenchWindow window) {
    currentWindow = window;
    
    var project = Adapters.adapt(selectedMarker.getResource().getProject(), ISonarLintProject.class);
    var file = Adapters.adapt(selectedMarker.getResource(), ISonarLintFile.class);
    var issueKey = selectedMarker.getAttribute(MarkerUtils.SONAR_MARKER_TRACKED_ISSUE_ID_ATTR, null);
    if (issueKey == null) {
      currentWindow.getShell().getDisplay()
        .asyncExec(() -> MessageDialog.openError(currentWindow.getShell(), "Re-Openning resolved Issue",
          "No issue key available"));
      return;
    }
    
    var markerType = tryGetMarkerType(selectedMarker, "Re-Openning resolved Issue, marker type not available");
    if (markerType == null) {
      return;
    }

    var checkJob = new Job("Check user permissions for setting the issue resolution") {
      private ReopenIssueResponse result;
      private ResolvedBinding resolvedBinding;

      @Override
      protected IStatus run(IProgressMonitor monitor) {
        var binding = getBinding(selectedMarker);
        if (binding.isPresent()) {
          resolvedBinding = binding.get();
          try {
            result = JobUtils.waitForFuture(monitor, SonarLintBackendService.get().getBackend().getIssueService()
              .reopenIssue(new ReopenIssueParams(resolvedBinding.getProjectBinding().connectionId(), issueKey)));
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

    JobUtils.scheduleAfterSuccess(checkJob, () -> afterCheckSuccessful(project, file, markerType, checkJob.result,
      checkJob.resolvedBinding));
    checkJob.schedule();
  }
  
  private void afterCheckSuccessful(ISonarLintProject project, ISonarLintFile file, String markerType,
    ReopenIssueResponse result, ResolvedBinding resolvedBinding) {
    var isSonarCloud = resolvedBinding.getEngineFacade().isSonarCloud();

    if (!result.isIssueReopened()) {
      currentWindow.getShell().getDisplay()
        .asyncExec(() -> MessageDialog.openError(currentWindow.getShell(),
          "Re-Openning resolved Issue on " + (isSonarCloud ? "SonarCloud" : "SonarQube"), ""));
      return;
    }

    currentWindow.getShell().getDisplay().asyncExec(() -> {
      var job = new ReOpenResolvedJob(project, (ConnectedEngineFacade) resolvedBinding.getEngineFacade(), file,
        markerType.equals(SonarLintCorePlugin.MARKER_TAINT_ID));
      job.schedule();
    });
  }
}
