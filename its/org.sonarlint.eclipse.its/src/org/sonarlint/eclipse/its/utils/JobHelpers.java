/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonarlint.eclipse.its.utils;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;

/**
 * Inspired by https://github.com/eclipse/m2e-core/blob/master/org.eclipse.m2e.tests.common/src/org/eclipse/m2e/tests/common/JobHelpers.java
 */
public final class JobHelpers {

  public static void waitForJobsToComplete(SWTWorkbenchBot bot) {
    bot.sleep(1000);
    try {
      waitForJobsToComplete(new NullProgressMonitor());
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  public static void waitForServerUpdateJob(SWTWorkbenchBot bot) {
    System.out.println("Wait for server update job to complete...");
    waitForJobs(SonarLintServerUpdateJobMatcher.INSTANCE, 60 * 1000);
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
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IJobManager jobManager = Job.getJobManager();
    jobManager.suspend();
    try {
      Job[] jobs = jobManager.find(null);
      for (int i = 0; i < jobs.length; i++) {
        if (jobs[i] instanceof WorkspaceJob || jobs[i].getClass().getName().endsWith("JREUpdateJob")) {
          jobs[i].join();
        }
      }
      jobManager.join("org.sonarlint.eclipse.projectJob", null);
      workspace.run(new IWorkspaceRunnable() {
        @Override
        public void run(IProgressMonitor monitor) {
        }
      }, workspace.getRoot(), 0, monitor);

    } finally {
      jobManager.resume();
    }

    waitForBuildJobs();
  }

  private static void waitForBuildJobs() throws InterruptedException {
    waitForJobs(BuildJobMatcher.INSTANCE, 60 * 1000);
    Job.getJobManager().join("org.sonarlint.eclipse.projectJob", null);
  }

  private static void waitForJobs(IJobMatcher matcher, int maxWaitMillis) {
    while (true) {
      Job job = getJob(matcher);
      if (job == null) {
        return;
      }
      try {
        System.out.println("Waiting for job " + job.getName() + " to complete...");
        job.join();
        System.out.println("Job " + job.getName() + " completed");
      } catch (InterruptedException e) {
        // ignore and loop
      }
    }
  }

  private static Job getJob(IJobMatcher matcher) {
    Job[] jobs = Job.getJobManager().find(null);
    for (Job job : jobs) {
      System.out.println("Background job: " + job.getName());
      if (matcher.matches(job)) {
        System.out.println("Matched!");
        return job;
      }
    }
    return null;
  }

  public static interface IJobMatcher {

    boolean matches(Job job);

  }

  static class BuildJobMatcher implements IJobMatcher {

    public static final IJobMatcher INSTANCE = new BuildJobMatcher();

    @Override
    public boolean matches(Job job) {
      return (job instanceof WorkspaceJob) || job.getClass().getName().matches("(.*\\.AutoBuild.*)")
        || job.getClass().getName().endsWith("JREUpdateJob");
    }

  }

  static class SonarLintServerUpdateJobMatcher implements IJobMatcher {

    public static final IJobMatcher INSTANCE = new SonarLintServerUpdateJobMatcher();

    @Override
    public boolean matches(Job job) {
      return job.getClass().getName().endsWith("ServerUpdateJob");
    }

  }

}
