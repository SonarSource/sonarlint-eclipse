/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.core.internal.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.sonar.ide.eclipse.core.internal.Messages;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.core.internal.servers.SonarServer;

public class SyncProjectJob extends Job {

  private final SonarProject sonarProject;
  private final SonarServer sonarServer;

  public SyncProjectJob(SonarProject sonarProject) {
    super("Synchronize caches of project " + sonarProject.getName());
    this.sonarProject = sonarProject;
    this.sonarServer = SonarCorePlugin.getServersManager().findServer(sonarProject.getUrl());
    setPriority(Job.LONG);
    // Prevent concurrent SQ analysis
    setRule(AnalyzeProjectJob.SONAR_ANALYSIS_RULE);
  }

  @Override
  protected IStatus run(final IProgressMonitor monitor) {
    // Verify Host
    if (getSonarServer() == null) {
      SonarCorePlugin.getDefault().error(NLS.bind(Messages.No_matching_server_in_configuration_for_project, sonarProject.getName(), sonarProject.getUrl()) + "\n");
      return Status.OK_STATUS;
    }
    // Verify version and server is reachable
    if (getSonarServer().disabled()) {
      SonarCorePlugin.getDefault().info("SonarQube server " + sonarProject.getUrl() + " is disabled");
      return Status.OK_STATUS;
    }

    try {
      getSonarServer().synchCaches(sonarProject.getKey());
    } catch (Exception e) {
      SonarCorePlugin.getDefault().error("Error during execution of SonarQube analysis", e);
      return new Status(Status.WARNING, SonarCorePlugin.PLUGIN_ID, "Error when executing SonarQube analysis", e);
    }
    if (monitor.isCanceled()) {
      return Status.CANCEL_STATUS;
    }

    return Status.OK_STATUS;
  }

  private SonarServer getSonarServer() {
    return sonarServer;
  }

}
