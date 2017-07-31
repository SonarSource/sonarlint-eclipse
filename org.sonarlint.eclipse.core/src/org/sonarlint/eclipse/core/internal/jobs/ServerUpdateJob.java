/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2017 SonarSource SA
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
import java.util.List;
import java.util.Map;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.client.api.connected.RemoteModule;

public class ServerUpdateJob extends Job {
  private final IServer server;

  public ServerUpdateJob(IServer server) {
    super("Update data from SonarQube server '" + server.getId() + "'");
    this.server = server;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    List<ISonarLintProject> projectsToUpdate = server.getBoundProjects();
    monitor.beginTask("Update server and all associated projects", projectsToUpdate.size() + 1);
    try {
      server.updateStorage(monitor);
    } catch (Exception e) {
      return new Status(IStatus.ERROR, SonarLintCorePlugin.PLUGIN_ID, "Unable to update data from server '" + server.getId() + "'", e);
    }
    monitor.worked(1);

    List<IStatus> failures = new ArrayList<>();
    for (ISonarLintProject projectToUpdate : projectsToUpdate) {
      if (monitor.isCanceled()) {
        return Status.CANCEL_STATUS;
      }
      try {
        SonarLintProjectConfiguration config = SonarLintProjectConfiguration.read(projectToUpdate.getScopeContext());
        fixProjectKeyIfMissing(config);
        server.updateProjectStorage(config.getModuleKey(), monitor);
      } catch (Exception e) {
        failures.add(new Status(IStatus.ERROR, SonarLintCorePlugin.PLUGIN_ID, "Unable to update binding for project '" + projectToUpdate.getName() + "'", e));
      }
      monitor.worked(1);
    }
    monitor.done();
    if (!failures.isEmpty()) {
      return new MultiStatus(SonarLintCorePlugin.PLUGIN_ID, IStatus.ERROR, failures.toArray(new IStatus[0]), "Failed to update binding for " + failures.size() + " project(s)",
        null);
    }
    return Status.OK_STATUS;
  }

  // note: this is only necessary for projects bound before SQ 6.6
  private void fixProjectKeyIfMissing(SonarLintProjectConfiguration config) {
    if (config.getProjectKey() == null) {
      RemoteModule remoteModule = server.getRemoteModules().get(config.getModuleKey());
      if (remoteModule != null) {
        config.setProjectKey(remoteModule.getProjectKey());
        config.save();
      }
    }
  }
}
