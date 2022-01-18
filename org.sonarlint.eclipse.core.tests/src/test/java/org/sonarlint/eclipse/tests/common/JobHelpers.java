/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2022 SonarSource SA
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
package org.sonarlint.eclipse.tests.common;

import java.util.function.Predicate;
import java.util.stream.Stream;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.Assert;

/**
 * Inspired by m2e
 *
 */
public class JobHelpers {

  private static final int POLLING_DELAY = 10;

  public static void waitForJobsToComplete() {
    try {
      waitForJobsToComplete(new NullProgressMonitor());
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  public static void waitForJobsToComplete(IProgressMonitor monitor) throws InterruptedException, CoreException {
    waitForBuildJobs();

    /*
     * First, make sure refresh job gets all resource change events
     * 
     * Resource change events are delivered after WorkspaceJob#runInWorkspace returns
     * and during IWorkspace#run. Each change notification is delivered by
     * only one thread/job, so we make sure no other workspaceJob is running then
     * call IWorkspace#run from this thread.
     * 
     * Unfortunately, this does not catch other jobs and threads that call IWorkspace#run
     * so we have to hard-code workarounds
     * 
     * See http://www.eclipse.org/articles/Article-Resource-deltas/resource-deltas.html
     */
    var workspace = ResourcesPlugin.getWorkspace();
    var jobManager = Job.getJobManager();
    jobManager.suspend();
    try {
      var jobs = jobManager.find(null);
      for (var i = 0; i < jobs.length; i++) {
        if (jobs[i] instanceof WorkspaceJob || jobs[i].getClass().getName().endsWith("JREUpdateJob")) {
          jobs[i].join();
        }
      }
      workspace.run(new IWorkspaceRunnable() {
        public void run(IProgressMonitor monitor) {
        }
      }, workspace.getRoot(), 0, monitor);

    } finally {
      jobManager.resume();
    }

    waitForBuildJobs();
  }

  private static void waitForBuildJobs() {
    waitForJobs(BuildJobMatcher.INSTANCE, 60 * 1000);
  }

  public static void waitForJobs(Predicate<Job> matcher, int maxWaitMillis) {
    final var limit = System.currentTimeMillis() + maxWaitMillis;
    while (true) {
      var job = getJob(matcher);
      if (job == null) {
        return;
      }
      var timeout = System.currentTimeMillis() > limit;
      Assert.assertFalse("Timeout while waiting for completion of job: " + job, timeout);
      job.wakeUp();
      try {
        Thread.sleep(POLLING_DELAY);
      } catch (InterruptedException e) {
        // ignore and keep waiting
      }
    }
  }

  private static Job getJob(Predicate<Job> matcher) {
    return Stream.of(Job.getJobManager().find(null))
      .filter(matcher)
      .findFirst()
      .orElse(null);
  }

  static class LaunchJobMatcher implements Predicate<Job> {

    public static final Predicate<Job> INSTANCE = new LaunchJobMatcher();

    public boolean test(Job job) {
      return job.getClass().getName().matches("(.*\\.DebugUIPlugin.*)");
    }

  }

  static class BuildJobMatcher implements Predicate<Job> {

    public static final Predicate<Job> INSTANCE = new BuildJobMatcher();

    public boolean test(Job job) {
      return (job instanceof WorkspaceJob) || job.getClass().getName().matches("(.*\\.AutoBuild.*)")
        || job.getClass().getName().endsWith("JREUpdateJob");
    }

  }

}
