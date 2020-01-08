/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2020 SonarSource SA
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.client.api.exceptions.CanceledException;

public class ServerUpdateJob extends Job {
  private final IServer server;

  public ServerUpdateJob(IServer server) {
    super("Update SonarLint binding data from '" + server.getId() + "'");
    this.server = server;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    List<ISonarLintProject> projectsToUpdate = server.getBoundProjects();
    monitor.beginTask("Update SonarLint binding data for all associated projects", projectsToUpdate.size() + 1);
    try {
      server.updateStorage(monitor);
    } catch (Exception e) {
      if (e instanceof CanceledException && monitor.isCanceled()) {
        return Status.CANCEL_STATUS;
      }
      return new Status(IStatus.ERROR, SonarLintCorePlugin.PLUGIN_ID, "Unable to update binding data from '" + server.getId() + "'", e);
    }
    monitor.worked(1);

    Set<String> seenProjectKeys = new HashSet<>();
    List<IStatus> failures = new ArrayList<>();
    for (ISonarLintProject projectToUpdate : projectsToUpdate) {
      if (monitor.isCanceled()) {
        return Status.CANCEL_STATUS;
      }
      try {
        SonarLintProjectConfiguration config = SonarLintCorePlugin.loadConfig(projectToUpdate);
        config.getProjectBinding().ifPresent(b -> {
          if (seenProjectKeys.add(b.projectKey())) {
            server.updateProjectStorage(b.projectKey(), monitor);
          }
        });

      } catch (Exception e) {
        if (e instanceof CanceledException && monitor.isCanceled()) {
          return Status.CANCEL_STATUS;
        }
        failures.add(new Status(IStatus.ERROR, SonarLintCorePlugin.PLUGIN_ID, "Unable to update binding data for project '" + projectToUpdate.getName() + "'", e));
      }
      monitor.worked(1);
    }
    monitor.done();
    if (!failures.isEmpty()) {
      return new MultiStatus(SonarLintCorePlugin.PLUGIN_ID, IStatus.ERROR, failures.toArray(new IStatus[0]), "Failed to update binding data for " + failures.size() + " project(s)",
        null);
    }
    return Status.OK_STATUS;
  }

}
