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
package org.sonarlint.eclipse.ui.internal.dialog;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.job.AwaitProjectConnectionReadyJob;

/**
 *  This dialog is linked to {@link org.sonarlint.eclipse.ui.internal.job.AwaitProjectConnectionReadyJob}!
 *
 *  This dialog will be shown to users when an action should be performed that requires a connection to be fully
 *  established. E.g. the "Open in IDE" feature that needs to run an analysis, that is only ready after the connection
 *  is fully working (analyzers, quality gates downloaded etc).
 */
public class AwaitProjectConnectionReadyDialog extends MessageDialog {
  private ClosingReason reason = ClosingReason.CLOSED_BY_USER;
  private boolean closed = false;

  public AwaitProjectConnectionReadyDialog(Shell parentShell) {
    super(parentShell,
      "Await Connected Mode to get ready",
      SonarLintImages.BALLOON_IMG,
      "In order to proceed we have to wait for the Connected Mode to get ready. Depending on your network connection "
        + "it can take some time, at most SonarLint will wait for " + AwaitProjectConnectionReadyJob.maxWaitTime()
        + " minute(s). When everything is finished this dialog will be closed automatically and SonarLint will "
        + "proceed. If you click 'Cancel', the Connected Mode will still be established but the requested issue will "
        + "not be displayed.",
      INFORMATION,
      new String[] {"Cancel"},
      0);
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    parent.getShell().addDisposeListener(e -> closed = true);
    return super.createDialogArea(parent);
  }

  public void closeByJob(boolean success) {
    reason = success ? ClosingReason.CLOSED_BY_JOB_SUCCESS : ClosingReason.CLOSED_BY_JOB_CANCEL;
  }

  public boolean cancelledByUser() {
    return reason.equals(ClosingReason.CLOSED_BY_USER);
  }

  public boolean cancelledByJob() {
    return reason.equals(ClosingReason.CLOSED_BY_JOB_CANCEL);
  }

  public boolean isClosed() {
    return closed;
  }

  private enum ClosingReason {
    CLOSED_BY_JOB_SUCCESS,
    CLOSED_BY_JOB_CANCEL,
    CLOSED_BY_USER
  }
}
