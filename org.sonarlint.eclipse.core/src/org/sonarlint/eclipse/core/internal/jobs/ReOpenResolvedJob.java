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
package org.sonarlint.eclipse.core.internal.jobs;

import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.sonarlint.eclipse.core.SonarLintNotifications;
import org.sonarlint.eclipse.core.SonarLintNotifications.Notification;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest.FileWithDocument;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

/** Job to be run after successfully re-opening a resolved issue (either server or anticipated one) */
public class ReOpenResolvedJob extends Job {
  private final ISonarLintProject project;
  private final boolean isTaint;
  private final ISonarLintFile file;

  public ReOpenResolvedJob(ISonarLintProject project, ISonarLintFile file,
    boolean isTaint) {
    super("Re-Opening resolved Issue");
    this.project = project;
    this.file = file;
    this.isTaint = isTaint;
    setPriority(INTERACTIVE);
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    SonarLintNotifications.get()
      .showNotification(new Notification("Re-Opening resolved Issue", "The issue was successfully re-opened", null));
    if (!isTaint) {
      var request = new AnalyzeProjectRequest(project, List.of(new FileWithDocument(file, null)),
        TriggerType.AFTER_RESOLVE, false);
      AnalyzeProjectJob.create(request).schedule();
    }
    return Status.OK_STATUS;
  }
}
