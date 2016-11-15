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

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectJob;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProject;
import org.sonarlint.eclipse.core.internal.server.IServer;

public class JobUtils {

  private JobUtils() {
    // utility class, forbidden constructor
  }

  // Make sure you only call this from within a Display, otherwise the workbench is not available
  // See: http://stackoverflow.com/questions/1265174/nullpointerexception-in-platformui-getworkbench-getactiveworkbenchwindow-get
  public static void scheduleAnalysisOfOpenFiles(IProject project) {
    List<IFile> files = new ArrayList<>();

    IWorkbenchWindow workbench = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if (workbench == null) {
      SonarLintCorePlugin.getDefault().debug("possible attempt to get workbench window outside of a Display");
      return;
    }

    for (IEditorReference ref : workbench.getActivePage().getEditorReferences()) {
      IEditorInput input;
      try {
        input = ref.getEditorInput();
        IFile file = getFileFromEditorInput(input);
        if (file != null && file.getProject().equals(project)) {
          files.add(file);
        }
      } catch (PartInitException e) {
        SonarLintCorePlugin.getDefault().warn("could not get editor content", e);
      }
    }

    if (!files.isEmpty()) {
      AnalyzeProjectRequest request = new AnalyzeProjectRequest(project, files, TriggerType.BINDING_CHANGE);
      new AnalyzeProjectJob(request).schedule();
    }
  }

  private static IFile getFileFromEditorInput(IEditorInput input) {
    if (input instanceof IFileEditorInput) {
      return ((IFileEditorInput) input).getFile();
    }
    return null;
  }

  public static void scheduleAnalysisOfOpenFiles(SonarLintProject project) {
    Runnable runnable = () -> scheduleAnalysisOfOpenFiles(project.getProject());
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

  public static void scheduleAnalysisOfOpenFiles(Job job, List<SonarLintProject> projects) {
    job.addJobChangeListener(new JobCompletionListener() {
      @Override
      public void done(IJobChangeEvent event) {
        if (event.getResult().isOK()) {
          scheduleAnalysisOfOpenFiles(projects);
        }
      }
    });
  }

  public static void scheduleAnalysisOfOpenFilesInBoundProjects(Job job, IServer server) {
    scheduleAnalysisOfOpenFiles(job, server.getBoundProjects());
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
