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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.sonarlint.eclipse.core.internal.backend.ConfigScopeSynchronizer;
import org.sonarlint.eclipse.core.internal.jobs.AnalysisReadyStatusCache;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.dialog.AwaitProjectConnectionReadyDialog;

/**
 *  This job is linked to the {@link org.sonarlint.eclipse.ui.internal.dialog.AwaitProjectConnectionReadyDialog}!
 *
 *  This job is running in the background of the dialog awaiting the analysis to get ready for the project indicating
 *  that the connection is now ready. It will trigger itself and takes at most 1 minute.
 */
public class AwaitProjectConnectionReadyJob extends Job {
  public static final int SCHEDULE_TIMER_MS = 250;
  public static final int SCHEDULE_ITERATIONS = 240;

  private final ISonarLintProject project;
  private final AwaitProjectConnectionReadyDialog dialog;
  private final int iteration;

  public AwaitProjectConnectionReadyJob(ISonarLintProject project, AwaitProjectConnectionReadyDialog dialog, int iteration) {
    super("Await connection to get ready in the background");
    this.project = project;
    this.dialog = dialog;
    this.iteration = iteration;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    if (dialog.isClosed()) {
      return Status.OK_STATUS;
    } else if (AnalysisReadyStatusCache.getAnalysisReadiness(ConfigScopeSynchronizer.getConfigScopeId(project))) {
      closeDialog(true);
      return Status.OK_STATUS;
    } else if (iteration >= SCHEDULE_ITERATIONS) {
      closeDialog(false);
      return Status.CANCEL_STATUS;
    }

    new AwaitProjectConnectionReadyJob(project, dialog, iteration + 1)
      .schedule(SCHEDULE_TIMER_MS);
    return Status.OK_STATUS;
  }

  private void closeDialog(boolean success) {
    dialog.closeByJob(success);
    Display.getDefault().syncExec(dialog::close);
  }

  public static int maxWaitTime() {
    return AwaitProjectConnectionReadyJob.SCHEDULE_ITERATIONS * AwaitProjectConnectionReadyJob.SCHEDULE_TIMER_MS / 60000;
  }
}
