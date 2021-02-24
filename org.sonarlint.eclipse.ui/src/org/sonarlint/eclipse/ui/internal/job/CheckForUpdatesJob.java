/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2021 SonarSource SA
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
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.connected.IConnectedEngineFacade;
import org.sonarlint.eclipse.ui.internal.popup.ServerUpdateAvailablePopup;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine.State;

public class CheckForUpdatesJob extends Job {

  public CheckForUpdatesJob() {
    super("Check for updates of binding data on SonarQube/SonarCloud");
    setPriority(DECORATE);
    setSystem(true);
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    try {
      SubMonitor subMonitor = SubMonitor.convert(monitor, SonarLintCorePlugin.getServersManager().getServers().size());
      subMonitor.setTaskName("Check for updates of binding data on SonarQube/SonarCloud");
      for (final IConnectedEngineFacade server : SonarLintCorePlugin.getServersManager().getServers()) {
        subMonitor.subTask("Checking for updates of binding data from server '" + server.getId() + "'");
        SubMonitor serverMonitor = subMonitor.newChild(1);

        IStatus status = checkForUpdates(server, serverMonitor);
        if (status.matches(Status.CANCEL)) {
          return status;
        }

        serverMonitor.done();
      }
      return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
    } finally {
      // Reschedule in 24H
      schedule((long) 24 * 60 * 60 * 1000);
    }
  }

  private static IStatus checkForUpdates(final IConnectedEngineFacade server, SubMonitor monitor) {
    // No need to check for remote updates if local storage is already outdated
    if (server.getStorageState() == State.UPDATED) {
      server.checkForUpdates(monitor);

      if (server.hasUpdates()) {
        Display.getDefault().asyncExec(() -> {
          ServerUpdateAvailablePopup popup = new ServerUpdateAvailablePopup(server);
          popup.open();
        });
      }

    }
    return Status.OK_STATUS;
  }

}
