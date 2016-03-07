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
package org.sonarlint.eclipse.ui.internal.server;

import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.ui.internal.Messages;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;

/**
 * Dialog that prompts a user to delete server(s).
 */
public class DeleteServerDialog extends MessageDialog {
  protected List<IServer> servers;

  /**
   * DeleteServerDialog constructor comment.
   * 
   * @param parentShell a shell
   * @param servers an array of servers
   * @param configs an array of server configurations
   */
  public DeleteServerDialog(Shell parentShell, List<IServer> servers) {
    super(parentShell, Messages.deleteServerDialogTitle, null, null, QUESTION, new String[] {IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL}, 0);

    if (servers == null) {
      throw new IllegalArgumentException();
    }

    this.servers = servers;

    if (servers.size() == 1) {
      message = NLS.bind(Messages.deleteServerDialogMessage, servers.get(0).getName());
    } else {
      message = NLS.bind(Messages.deleteServerDialogMessageMany, Integer.toString(servers.size()));
    }
  }

  @Override
  protected void buttonPressed(int buttonId) {
    if (buttonId == OK) {

      Job job = new Job(Messages.deleteServerTask) {
        @Override
        protected IStatus run(IProgressMonitor monitor) {
          if (servers.isEmpty()) {
            // all servers have been deleted from list
            return Status.OK_STATUS;
          }
          try {
            if (monitor.isCanceled()) {
              return Status.CANCEL_STATUS;
            }

            for (IServer server : servers) {
              server.delete();
            }

            if (monitor.isCanceled()) {
              return Status.CANCEL_STATUS;
            }
          } catch (Exception e) {
            return new Status(IStatus.ERROR, SonarLintUiPlugin.PLUGIN_ID, 0, e.getMessage(), e);
          }

          return Status.OK_STATUS;
        }
      };

      job.setPriority(Job.BUILD);

      job.schedule();
    }
    super.buttonPressed(buttonId);
  }
}
