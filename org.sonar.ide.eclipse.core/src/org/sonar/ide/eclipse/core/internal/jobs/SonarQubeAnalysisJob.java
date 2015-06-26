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

import java.util.List;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.wsclient.ConnectionException;

public class SonarQubeAnalysisJob extends Job {

  private IProgressMonitor monitor;
  private List<AnalyzeProjectRequest> requests;

  public SonarQubeAnalysisJob(List<AnalyzeProjectRequest> requests) {
    super("Synchronize issues");
    this.requests = requests;
    setPriority(Job.LONG);
  }

  @Override
  protected IStatus run(final IProgressMonitor monitor) {
    this.monitor = monitor;
    IStatus status;
    try {
      monitor.beginTask("Synchronize", requests.size());

      for (final AnalyzeProjectRequest request : requests) {
        if (monitor.isCanceled()) {
          break;
        }
        IProject project = request.getResource().getProject();
        if (project.isAccessible()) {
          monitor.subTask(project.getName());
          scheduleAnalysis(request);
        }
        monitor.worked(1);
      }

      if (!monitor.isCanceled()) {
        status = Status.OK_STATUS;
      } else {
        status = Status.CANCEL_STATUS;
      }
    } catch (final ConnectionException e) {
      status = new Status(IStatus.ERROR, SonarCorePlugin.PLUGIN_ID, IStatus.ERROR, "Unable to contact SonarQube server", e);
    } catch (final Exception e) {
      status = new Status(IStatus.ERROR, SonarCorePlugin.PLUGIN_ID, IStatus.ERROR, e.getLocalizedMessage(), e);
    } finally {
      monitor.done();
    }
    return status;
  }

  private void scheduleAnalysis(AnalyzeProjectRequest request) throws InterruptedException {
    AnalyzeProjectJob analyzeProjectJob = new AnalyzeProjectJob(request);
    analyzeProjectJob.schedule();
    analyzeProjectJob.join();
  }

  public IProgressMonitor getMonitor() {
    return monitor;
  }

}
