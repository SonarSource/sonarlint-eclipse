/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
 * dev@sonar.codehaus.org
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
package org.sonar.ide.eclipse.core.internal.jobs.functions;

import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.sonar.ide.eclipse.common.issues.ISonarIssueWithPath;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.jobs.AnalyseProjectRequest;
import org.sonar.ide.eclipse.core.internal.markers.MarkerUtils;
import org.sonar.ide.eclipse.core.internal.markers.SonarMarker;
import org.sonar.ide.eclipse.core.internal.remote.EclipseSonar;
import org.sonar.ide.eclipse.core.internal.remote.SourceCode;
import org.sonar.ide.eclipse.core.internal.resources.ResourceUtils;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.wsclient.ConnectionException;

/**
 *
 * @author Hemantkumar Chigadani
 */
@SuppressWarnings("nls")
public abstract class SynchronizeAllIssuesJobFuction implements IJobFunction, IIncrementalAnalysisJobFunction {

  private final List<AnalyseProjectRequest> requests;

  /**
   * @param requests
   */
  public SynchronizeAllIssuesJobFuction(final List<AnalyseProjectRequest> requests) {
    this.requests = requests;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IStatus run(final IProgressMonitor monitor) {
    IStatus status;
    try {
      monitor.beginTask("Synchronize", requests.size());
      for (final AnalyseProjectRequest request : requests) {
        if (monitor.isCanceled()) {
          break;
        }
        final IProject project = request.getProject();
        // TODO Sonar projects with corresponding live SonarQube servers should be executed and report to user failed projects.
        if (project.isAccessible())
        {
          monitor.subTask(project.getName());
          // http://jira.sonarsource.com/browse/SONARCLIPS-403
          // Delete markers and persistence properties for a given resource as and then resource is analysed, not at project level.
          // User may loss existing markers cache if server is offline.
          fetchRemoteIssues(project, monitor);
          scheduleIncrementalAnalysis(request);
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

  private void fetchRemoteIssues(final IProject project, final IProgressMonitor monitor) throws CoreException {
    final long start = System.currentTimeMillis();
    SonarCorePlugin.getDefault().info("Retrieve remote issues of project " + project.getName() + "...\n");

    final SonarProject sonarProject = SonarProject.getInstance(project);
    if (monitor.isCanceled()) {
      return;
    }
    final EclipseSonar sonar = EclipseSonar.getInstance(project);
    if (sonar == null) {
      return;
    }
    final SourceCode sourceCode = sonar.search(project);

    if (monitor.isCanceled()) {
      return;
    }

    if (sourceCode != null) {
      doRefreshIssues(sonarProject, sourceCode, monitor);
      sonarProject.setLastAnalysisDate(sourceCode.getAnalysisDate());
      sonarProject.save();
    } else {
      SonarCorePlugin.getDefault().error("Project not found on remote SonarQube server [" + sonarProject.getKey() + "]\n");
    }
    SonarCorePlugin.getDefault().debug("Done in " + (System.currentTimeMillis() - start) + "ms\n");
  }

  private void doRefreshIssues(final SonarProject sonarProject, final SourceCode sourceCode, final IProgressMonitor monitor) throws CoreException {
    final long start = System.currentTimeMillis();
    final List<ISonarIssueWithPath> issues = sourceCode.getRemoteIssuesRecursively(monitor);
    SonarCorePlugin.getDefault().debug("  WS call took " + (System.currentTimeMillis() - start) + "ms for " + issues.size() + " issues\n");
    for (final ISonarIssueWithPath issue : issues) {
      final IResource eclipseResource = ResourceUtils.findResource(sonarProject, issue.resourceKey());
      if (eclipseResource instanceof IFile)
      {
        // Delete markers on current resource now.
        MarkerUtils.deleteIssuesMarkers(eclipseResource);
        // Fetch new remote markers
        SonarMarker.create(eclipseResource, false, issue);
      }
      if (monitor.isCanceled()) {
        return;
      }
    }
  }

}
