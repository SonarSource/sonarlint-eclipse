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
package org.sonarlint.eclipse.ui.internal.job;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.core.internal.server.ServersManager;
import org.sonarlint.eclipse.ui.internal.popup.ServerUpdateAvailablePopup;

public class CheckForUpdatesJob extends Job {

  public CheckForUpdatesJob() {
    super("Check for configuration update on SonarQube servers");
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    for (final IServer server : ServersManager.getInstance().getServers()) {
      // No need to check for remote updates if local storage is already outdated
      if (server.isStorageUpdated()) {
        server.checkForUpdates(monitor);
        if (server.hasUpdates()) {
          Display.getDefault().asyncExec(() -> {
            ServerUpdateAvailablePopup popup = new ServerUpdateAvailablePopup(Display.getCurrent(), server);
            popup.create();
            popup.open();
          });
        }
      }
    }
    // Reschedule in 24H
    schedule(24 * 60 * 60 * 1000);
    return Status.OK_STATUS;
  }

}
