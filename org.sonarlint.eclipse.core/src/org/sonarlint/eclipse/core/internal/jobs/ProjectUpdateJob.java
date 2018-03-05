/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2018 SonarSource SA
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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

public class ProjectUpdateJob extends Job {
  private final ISonarLintProject project;

  public ProjectUpdateJob(ISonarLintProject project) {
    super("Update configuration of project " + project.getName());
    this.project = project;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    try {
      SonarLintProjectConfiguration config = SonarLintProjectConfiguration.read(project.getScopeContext());
      String serverId = config.getServerId();
      IServer server = SonarLintCorePlugin.getServersManager().getServer(serverId);
      if (server == null) {
        return new Status(IStatus.ERROR, SonarLintCorePlugin.PLUGIN_ID,
          "Unable to update project '" + project.getName() + "' since it is bound to an unknow server: '" + serverId + "'");
      }
      server.updateProjectStorage(config.getModuleKey(), monitor);
      return Status.OK_STATUS;
    } catch (Exception e) {
      return new Status(IStatus.ERROR, SonarLintCorePlugin.PLUGIN_ID, "Unable to update project " + project.getName(), e);
    }
  }
}
