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

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.sonar.ide.eclipse.core.internal.Messages;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.core.internal.servers.SonarServer;

public abstract class AbstractSonarProjectJob extends Job {

  private final SonarProject sonarProject;

  private static final ISchedulingRule SONAR_ANALYSIS_RULE = ResourcesPlugin.getWorkspace().getRuleFactory().buildRule();

  public AbstractSonarProjectJob(String title, SonarProject project) {
    super(title);
    this.sonarProject = project;
    setPriority(Job.DECORATE);
    // Prevent concurrent SQ analysis
    setRule(SONAR_ANALYSIS_RULE);
  }

  @Override
  protected final IStatus run(final IProgressMonitor monitor) {
    SonarServer serverToUse;
    if (!sonarProject.isAssociated()) {
      serverToUse = SonarCorePlugin.getServersManager().getDefaultServer();
      if (serverToUse == null) {
        SonarCorePlugin.getDefault()
          .error(Messages.No_default_server + System.lineSeparator());
        return new Status(Status.WARNING, SonarCorePlugin.PLUGIN_ID, Messages.No_default_server);
      }
    } else {
      serverToUse = sonarProject.getServer();
    }

    // Verify Host
    if (serverToUse == null) {
      SonarCorePlugin.getDefault()
        .error(NLS.bind(Messages.No_matching_server_in_configuration_for_project, sonarProject.getName(), sonarProject.getServerId()) + System.lineSeparator());
      return Status.CANCEL_STATUS;
    }
    // Verify version and server is reachable
    if (!serverToUse.started()) {
      SonarCorePlugin.getDefault().info("SonarQube server " + serverToUse.getId() + " is disabled" + System.lineSeparator());
      return Status.CANCEL_STATUS;
    }

    return run(serverToUse, monitor);
  }

  protected SonarProject getSonarProject() {
    return sonarProject;
  }

  protected abstract IStatus run(SonarServer serverToUse, final IProgressMonitor monitor);

}
