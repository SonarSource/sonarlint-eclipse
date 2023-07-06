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

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.SonarLintNotifications;
import org.sonarlint.eclipse.core.SonarLintNotifications.Notification;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.backend.SonarLintBackendService;
import org.sonarlint.eclipse.core.internal.utils.JobUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.clientapi.backend.issue.IssueStatus;

public class MarkAsResolvedJob extends Job {

  private final ISonarLintProject project;
  private final String serverIssueKey;
  private final IssueStatus newStatus;
  private final boolean isTaint;
  private final @Nullable String comment;

  public MarkAsResolvedJob(ISonarLintProject project, String serverIssueKey, IssueStatus newStatus, @Nullable String comment, boolean isTaint) {
    super("Marking issue as resolved");
    this.project = project;
    this.serverIssueKey = serverIssueKey;
    this.newStatus = newStatus;
    this.comment = comment;
    this.isTaint = isTaint;
    setPriority(INTERACTIVE);
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    try {
      monitor.subTask("Change issue status to " + newStatus);
      JobUtils.waitForFuture(monitor, SonarLintBackendService.get().changeIssueStatus(project, serverIssueKey, newStatus, isTaint));
      if (comment != null) {
        monitor.subTask("Add comment");
        JobUtils.waitForFuture(monitor, SonarLintBackendService.get().addIssueComment(project, serverIssueKey, comment));
      }
      SonarLintNotifications.get()
        .showNotification(new Notification("Issue marked as resolved", "The issue was successfully marked as resolved", null));
      return Status.OK_STATUS;
    } catch (InvocationTargetException e) {
      return new Status(Status.ERROR, SonarLintCorePlugin.PLUGIN_ID, e.getMessage(), e);
    } catch (InterruptedException e) {
      return new Status(Status.CANCEL, SonarLintCorePlugin.PLUGIN_ID, e.getMessage(), e);
    }

  }
}
