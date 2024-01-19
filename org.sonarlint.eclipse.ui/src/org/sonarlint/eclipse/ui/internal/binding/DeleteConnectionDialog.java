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
package org.sonarlint.eclipse.ui.internal.binding;

import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.engine.connected.IConnectedEngineFacade;
import org.sonarlint.eclipse.ui.internal.Messages;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarlint.eclipse.ui.internal.binding.actions.AnalysisJobsScheduler;

/**
 * Dialog that prompts a user to delete server(s).
 */
public class DeleteConnectionDialog extends MessageDialog {
  protected List<IConnectedEngineFacade> servers;

  public DeleteConnectionDialog(Shell parentShell, List<IConnectedEngineFacade> servers) {
    super(parentShell, Messages.deleteServerDialogTitle, null, getMessage(servers), getImage(servers), new String[] {IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL}, 0);
    this.servers = servers;

  }

  private static int getImage(List<IConnectedEngineFacade> servers) {
    for (var iServer : servers) {
      if (!iServer.getBoundProjects().isEmpty()) {
        return WARNING;
      }
    }
    return QUESTION;
  }

  private static String getMessage(List<IConnectedEngineFacade> servers) {
    var sb = new StringBuilder();
    if (servers.size() == 1) {
      sb.append(NLS.bind(Messages.deleteServerDialogMessage, servers.get(0).getId()));
    } else {
      sb.append(NLS.bind(Messages.deleteServerDialogMessageMany, Integer.toString(servers.size())));
    }
    var boundCount = 0;
    for (var iServer : servers) {
      boundCount += iServer.getBoundProjects().size();
    }
    if (boundCount > 0) {
      sb.append("\n").append(NLS.bind(Messages.deleteServerDialogBoundProject, boundCount));
    }
    return sb.toString();
  }

  private class DeleteServerJob extends Job {
    DeleteServerJob() {
      super(Messages.deleteServerTask);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      try {
        for (var server : servers) {
          if (monitor.isCanceled()) {
            return Status.CANCEL_STATUS;
          }
          var boundProjects = server.getBoundProjects();
          server.delete();
          // All bound projects have been unbound, so refresh issues
          boundProjects.forEach(p -> AnalysisJobsScheduler.scheduleAnalysisOfOpenFiles(p, TriggerType.BINDING_CHANGE));
        }
      } catch (Exception e) {
        return new Status(IStatus.ERROR, SonarLintUiPlugin.PLUGIN_ID, 0, e.getMessage(), e);
      }
      return Status.OK_STATUS;
    }
  }

  @Override
  protected void buttonPressed(int buttonId) {
    if (buttonId == OK && !servers.isEmpty()) {
      var job = new DeleteServerJob();
      servers.forEach(server -> AnalysisJobsScheduler.scheduleAnalysisOfOpenFiles(job, server.getBoundProjects(), TriggerType.BINDING_CHANGE, true));
      job.setPriority(Job.BUILD);
      job.schedule();
    }
    super.buttonPressed(buttonId);
  }
}
