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

import java.util.ArrayList;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.connected.IConnectedEngineFacade;
import org.sonarsource.sonarlint.core.commons.progress.CanceledException;

public class ConnectionStorageUpdateJob extends Job {
  private final IConnectedEngineFacade server;

  public ConnectionStorageUpdateJob(IConnectedEngineFacade server) {
    super("Update SonarLint local storage for connection '" + server.getId() + "'");
    this.server = server;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    var projectKeysToUpdate = server.getBoundProjectKeys();
    monitor.beginTask("Update SonarLint local storage for all associated projects", projectKeysToUpdate.size());

    var failures = new ArrayList<IStatus>();
    for (var projectKeyToUpdate : projectKeysToUpdate) {
      if (monitor.isCanceled()) {
        return Status.CANCEL_STATUS;
      }
      try {
        server.updateProjectStorage(projectKeyToUpdate, monitor);
      } catch (Exception e) {
        if (e instanceof CanceledException && monitor.isCanceled()) {
          return Status.CANCEL_STATUS;
        }
        failures.add(newUpdateFailedStatus(projectKeyToUpdate, e));
      }
      monitor.worked(1);
    }

    server.manualSync(projectKeysToUpdate, monitor);
    server.notifyAllListenersStateChanged();

    monitor.done();
    if (!failures.isEmpty()) {
      var message = String.format("Failed to update local storage for %d project(s)", failures.size());
      return new MultiStatus(SonarLintCorePlugin.PLUGIN_ID, IStatus.ERROR, failures.toArray(new IStatus[0]), message, null);
    }
    return Status.OK_STATUS;
  }

  private static Status newUpdateFailedStatus(String projectKeyToUpdate, Exception e) {
    var message = String.format("Unable to update local storage for project '%s'", projectKeyToUpdate);
    return new Status(IStatus.ERROR, SonarLintCorePlugin.PLUGIN_ID, message, e);
  }

}
