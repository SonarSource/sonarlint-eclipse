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
package org.sonar.ide.eclipse.core.internal.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.jobs.AnalyseProjectRequest;
import org.sonar.ide.eclipse.core.internal.jobs.functions.AnalyzeProjectJobFunction;
import org.sonar.ide.eclipse.core.internal.jobs.functions.IJobFunction;
import org.sonar.ide.eclipse.core.internal.jobs.functions.SynchronizeAllIssuesJobFuction;
import org.sonar.ide.eclipse.core.internal.jobs.functions.SynchronizeIssuesJobFunction;
import org.sonar.ide.eclipse.core.internal.resources.SonarProperty;

/**
 * This class is abstraction between builder background worker job {@link SilenceJobWrapper} and build requests {@link AnalyseProjectRequest}s.
 * <br>
 * This maintains a single background worker per builder {@link SonarBuilder} which helps in keeping Sonar building process simple without
 * blocking the user while editing the source code. Build requests for a given project are be queued if more than one requests are made at 
 * a given time.
 * <br>
 * Build requests for a given resource in a project can be filtered if they are same compared to the queued requests.
 *
 * @author Hemantkumar Chigadani
 */
final class BuildWatcher {

  private final BlockingQueue<AnalyseProjectRequest> queue = new LinkedBlockingQueue<AnalyseProjectRequest>();

  /**
   * Single worker per builder.
   */
  private volatile Job worker;

  /**
   * Check worker state.
   */
  private void check() {
    synchronized (this) {
      if (worker == null) {
        schedueWorker();
      }
    }
  }

  /**
   * Note :Duplicate requests are filtered out.
   * 
   * @param analyseProjectRequest Request to be submitted
   */
  @SuppressWarnings("nls")
  synchronized void submit(final AnalyseProjectRequest analyseProjectRequest) {
    if (!queue.contains(analyseProjectRequest)) {
      SonarCorePlugin.getDefault().debug(BuildWatcher.class.getSimpleName() + " request queue: " + queue + "\n");
      queue.add(analyseProjectRequest);
      check();
    }

  }

  @SuppressWarnings("nls")
  private synchronized Job schedueWorker() {
    try {
      final AnalyseProjectRequest pollRequ = queue.poll(1, TimeUnit.SECONDS);
      if (pollRequ != null && (worker == null || worker.getState() == Job.NONE)) {

        if (pollRequ.isForceFullPreview()) {
          // Silence full build request
          final IJobFunction synchronizeAllIssuesJobFunction = new SynchronizeAllIssuesJobFuction(Arrays.asList(pollRequ)) {

            @Override
            public void scheduleIncrementalAnalysis(final AnalyseProjectRequest req) {
              new SilenceJobWrapper(new AnalyzeProjectJobFunction(pollRequ)).schedule();
            }
          };
          worker = new SilenceJobWrapper(synchronizeAllIssuesJobFunction);

        } else {
          // Use the same delta changes for both ,to fetch remote issues and to analyse issue locally.
          final AnalyzeProjectJobFunction analyzeProjectJobFunct = new AnalyzeProjectJobFunction(pollRequ);
          // Remote Sonar server issues fetcher job.
          final SynchronizeIssuesJobFunction synchronizeIssuesJobFunct = new SynchronizeIssuesJobFunction(getResources(pollRequ), false);
          synchronizeIssuesJobFunct.setRecursiveVisit(false);
          worker = new SilenceJobWrapper(synchronizeIssuesJobFunct, analyzeProjectJobFunct);

        }
        worker.addJobChangeListener(new JobChangeAdapter() {
          /**
           * {@inheritDoc}
           */
          @Override
          public void done(final IJobChangeEvent event) {
            final SilenceJobWrapper job = (SilenceJobWrapper) event.getJob();
            if (!job.isRescheduled()) {
              super.done(event);
              SonarCorePlugin.getDefault().debug("Completed build request fro queue: " + pollRequ + "\n");
              worker.removeJobChangeListener(this);
              schedueWorker();
            }
          }
        });
        worker.schedule();

      } else {
        worker = null;
      }
    } catch (final InterruptedException exception) {
      worker = null;
      SonarCorePlugin.getDefault().debug(exception.getMessage());
    }
    return worker;
  }

  /**
   * @param requ Build request
   * @return Parse the request to fetch changed resources location.
   */
  private List<IResource> getResources(final AnalyseProjectRequest requ) {
    final List<SonarProperty> extraProps = requ.getExtraProps();
    final List<IResource> resources = new ArrayList<IResource>();
    final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    for (final SonarProperty sonarProperty : extraProps) {
      final String value = sonarProperty.getValue();
      final IPath fromOSString = Path.fromOSString(value);
      if (fromOSString != null) {
        final IResource member = root.findMember(fromOSString);
        resources.add(member);

      }
    }
    return resources;
  }

}
