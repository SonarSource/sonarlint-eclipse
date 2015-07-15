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
package org.sonar.ide.eclipse.core.internal.jobs;

import java.util.Arrays;
import java.util.List;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.sonar.ide.eclipse.core.internal.jobs.functions.SynchronizeAllIssuesJobFuction;
import org.sonar.ide.eclipse.core.internal.resources.SonarProperty;

public class SynchronizeAllIssuesJob extends Job {

  private IProgressMonitor monitor;

  private final SynchronizeAllIssuesJobFuction synchronizeAllIssuesJobFuction;

  public static void createAndSchedule(final IProject project, final boolean debugEnabled, final List<SonarProperty> extraProps, final String jvmArgs,
    final boolean forceFullPreview) {
    final AnalyseProjectRequest request = new AnalyseProjectRequest(project)
      .setDebugEnabled(debugEnabled)
      .setExtraProps(extraProps)
      .setJvmArgs(jvmArgs)
      .setForceFullPreview(forceFullPreview);
    new SynchronizeAllIssuesJob(Arrays.asList(request)).schedule();
  }

  public SynchronizeAllIssuesJob(final List<AnalyseProjectRequest> requests) {
    super("Synchronize all issues");

    synchronizeAllIssuesJobFuction = new SynchronizeAllIssuesJobFuction(requests) {

      @Override
      public void scheduleIncrementalAnalysis(final AnalyseProjectRequest request) {
        new AnalyzeProjectJob(request).schedule();
      }
    };
    setPriority(Job.LONG);
  }

  @Override
  protected IStatus run(final IProgressMonitor monitor) {
    this.monitor = monitor;
    return synchronizeAllIssuesJobFuction.run(monitor);
  }

  public IProgressMonitor getMonitor() {
    return monitor;
  }

}
