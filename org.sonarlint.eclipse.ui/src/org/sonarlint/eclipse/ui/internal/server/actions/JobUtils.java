/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.server.actions;

import java.util.List;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProject;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.ui.internal.bind.BindUtils;

public class JobUtils {

  private JobUtils() {
    // utility class, forbidden constructor
  }

  public static void scheduleAnalysisOfOpenFiles(SonarLintProject project) {
    Runnable runnable = () -> BindUtils.scheduleAnalysisOfOpenFiles(project.getProject());
    if (Display.getCurrent() != null) {
      runnable.run();
    } else {
      Display.getDefault().asyncExec(runnable);
    }
  }

  public static void scheduleAnalysisOfOpenFiles(List<SonarLintProject> projects) {
    projects.forEach(JobUtils::scheduleAnalysisOfOpenFiles);
  }

  public static void scheduleAnalysisOfOpenFilesInBoundProjects(IServer server) {
    scheduleAnalysisOfOpenFiles(server.getBoundProjects());
  }

  public static void scheduleAnalysisOfOpenFilesAfter(Job job, List<SonarLintProject> projects) {
    job.addJobChangeListener(new JobCompletionListener() {
      @Override
      public void done(IJobChangeEvent event) {
        scheduleAnalysisOfOpenFiles(projects);
      }
    });
  }

  public static void scheduleAnalysisOfOpenFilesInBoundProjectsAfter(Job job, IServer server) {
    scheduleAnalysisOfOpenFilesAfter(job, server.getBoundProjects());
  }

  static class ServerUpdateJobListener extends JobCompletionListener {
    private final IServer server;

    private ServerUpdateJobListener(IServer server) {
      this.server = server;
    }

    @Override
    public void done(IJobChangeEvent event) {
      for (SonarLintProject project : server.getBoundProjects()) {
        Display.getDefault().asyncExec(() -> BindUtils.scheduleAnalysisOfOpenFiles(project.getProject()));
      }
    }
  }

  abstract static class JobCompletionListener implements IJobChangeListener {
    @Override
    public void aboutToRun(IJobChangeEvent event) {
      // nothing to do
    }

    @Override
    public void awake(IJobChangeEvent event) {
      // nothing to do
    }

    @Override
    public void running(IJobChangeEvent event) {
      // nothing to do
    }

    @Override
    public void scheduled(IJobChangeEvent event) {
      // nothing to do
    }

    @Override
    public void sleeping(IJobChangeEvent event) {
      // nothing to do
    }
  }
}
