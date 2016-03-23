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
package org.sonarlint.eclipse.ui.internal.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.sonarlint.eclipse.core.internal.builder.SonarLintBuilder;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectJob;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarlint.eclipse.ui.internal.views.issues.IssuesView;

public class AnalyzeCommand extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    final Map<IProject, Collection<IFile>> filesPerProject = findSelectedFilesPerProject(event);
    if (filesPerProject.isEmpty()) {
      IFile editedFile = findEditedFile(event);
      if (editedFile != null) {
        filesPerProject.put(editedFile.getProject(), Arrays.asList(editedFile));
      }
    }

    if (!filesPerProject.isEmpty()) {
      runAnalysisJobs(filesPerProject);
    }

    return null;
  }

  private void runAnalysisJobs(Map<IProject, Collection<IFile>> filesPerProject) {
    for (Map.Entry<IProject, Collection<IFile>> entry : filesPerProject.entrySet()) {
      AnalyzeProjectJob job = new AnalyzeProjectJob(new AnalyzeProjectRequest(entry.getKey(), entry.getValue()));
      job.schedule();
      showIssuesViewAfterJobSuccess(job);
    }
  }

  protected Map<IProject, Collection<IFile>> findSelectedFilesPerProject(ExecutionEvent event) throws ExecutionException {
    Map<IProject, Collection<IFile>> selectedFilesPerProject = new LinkedHashMap<>();
    ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);

    if (selection instanceof IStructuredSelection) {
      Object[] elems = ((IStructuredSelection) selection).toArray();
      collectFilesPerProject(selectedFilesPerProject, elems);
    }

    return selectedFilesPerProject;
  }

  private static void collectFilesPerProject(final Map<IProject, Collection<IFile>> filesToAnalyzePerProject, Object[] elems) {
    for (Object elem : elems) {
      if (elem instanceof IResource) {
        collectChildren(filesToAnalyzePerProject, (IResource) elem);
      } else if (elem instanceof IAdaptable && ((IAdaptable) elem).getAdapter(IResource.class) != null) {
        collectChildren(filesToAnalyzePerProject, (IResource) ((IAdaptable) elem).getAdapter(IResource.class));
      } else if (elem instanceof IWorkingSet) {
        IWorkingSet ws = (IWorkingSet) elem;
        collectFilesPerProject(filesToAnalyzePerProject, ws.getElements());
      }
    }
  }

  private static void collectChildren(final Map<IProject, Collection<IFile>> filesToAnalyzePerProject, IResource elem) {
    try {
      elem.accept(new IResourceVisitor() {

        @Override
        public boolean visit(IResource resource) throws CoreException {
          if (!SonarLintBuilder.shouldAnalyze(resource)) {
            return false;
          }
          IFile file = (IFile) resource.getAdapter(IFile.class);
          if (file == null) {
            // visit children too
            return true;
          }
          IProject project = resource.getProject();
          if (!filesToAnalyzePerProject.containsKey(project)) {
            filesToAnalyzePerProject.put(project, new ArrayList<IFile>());
          }
          filesToAnalyzePerProject.get(project).add(file);
          return true;
        }
      });
    } catch (CoreException e) {
      throw new IllegalStateException("Unable to collect files to analyze", e);
    }
  }

  protected static IFile findEditedFile(ExecutionEvent event) {
    IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
    IEditorInput input = activeEditor.getEditorInput();
    if (input instanceof IFileEditorInput) {
      return ((IFileEditorInput) input).getFile();
    }
    return null;
  }

  protected void showIssuesViewAfterJobSuccess(Job job) {
    // Display issues view after analysis is completed
    job.addJobChangeListener(new JobChangeAdapter() {
      @Override
      public void done(IJobChangeEvent event) {
        if (Status.OK_STATUS == event.getResult()) {
          Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
              IWorkbenchWindow iw = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
              try {
                iw.getActivePage().showView(IssuesView.ID, null, IWorkbenchPage.VIEW_VISIBLE);
              } catch (PartInitException e) {
                SonarLintUiPlugin.getDefault().getLog().log(new Status(Status.ERROR, SonarLintUiPlugin.PLUGIN_ID, Status.OK, "Unable to open Issues View", e));
              }
            }
          });
        }
      }
    });
    job.schedule();
  }

}
