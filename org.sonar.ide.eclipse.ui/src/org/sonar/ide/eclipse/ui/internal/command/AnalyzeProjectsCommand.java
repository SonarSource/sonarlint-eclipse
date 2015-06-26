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
package org.sonar.ide.eclipse.ui.internal.command;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
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
import org.sonar.ide.eclipse.core.internal.SonarNature;
import org.sonar.ide.eclipse.core.internal.jobs.AnalyzeProjectRequest;
import org.sonar.ide.eclipse.core.internal.jobs.SonarQubeAnalysisJob;
import org.sonar.ide.eclipse.ui.internal.SonarUiPlugin;
import org.sonar.ide.eclipse.ui.internal.console.SonarConsole;
import org.sonar.ide.eclipse.ui.internal.views.issues.IssuesView;

public class AnalyzeProjectsCommand extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    List<IProject> selectedProjects = Lists.newArrayList();

    findSelectedProjects(event, selectedProjects);
    if (selectedProjects.isEmpty()) {
      findProjectOfSelectedEditor(event, selectedProjects);
    }

    if (!selectedProjects.isEmpty()) {
      runAnalysisJob(selectedProjects);
    }

    return null;
  }

  private void runAnalysisJob(List<IProject> selectedProjects) {
    boolean debugEnabled = SonarConsole.isDebugEnabled();
    List<AnalyzeProjectRequest> requests = new ArrayList<AnalyzeProjectRequest>();
    SonarUiPlugin.getDefault().getSonarConsole().clearConsole();
    for (IProject project : selectedProjects) {
      if (!SonarNature.hasSonarNature(project)) {
        break;
      }
      requests.add(new AnalyzeProjectRequest(project)
        .setDebugEnabled(debugEnabled)
        .useHttpWsCache(false));
    }
    SonarQubeAnalysisJob job = new SonarQubeAnalysisJob(requests);
    showIssuesViewAfterJobSuccess(job);
  }

  private void findSelectedProjects(ExecutionEvent event, List<IProject> selectedProjects) throws ExecutionException {
    ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);

    if (selection instanceof IStructuredSelection) {
      Object[] elems = ((IStructuredSelection) selection).toArray();
      collectProjects(selectedProjects, elems);
    }
  }

  private void collectProjects(List<IProject> selectedProjects, Object[] elems) {
    for (Object elem : elems) {
      if (elem instanceof IProject) {
        selectedProjects.add((IProject) elem);
      } else if (elem instanceof IWorkingSet) {
        IWorkingSet ws = (IWorkingSet) elem;
        collectProjects(selectedProjects, ws.getElements());
      } else if (elem instanceof IAdaptable) {
        IProject proj = (IProject) ((IAdaptable) elem).getAdapter(IProject.class);
        if (proj != null) {
          selectedProjects.add(proj);
        }
      }
    }
  }

  private static void findProjectOfSelectedEditor(ExecutionEvent event, List<IProject> selectedProjects) {
    IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
    IEditorInput input = activeEditor.getEditorInput();
    if (input instanceof IFileEditorInput) {
      IFile currentFile = ((IFileEditorInput) input).getFile();
      selectedProjects.add(currentFile.getProject());
    }
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
                SonarUiPlugin.getDefault().getLog().log(new Status(Status.ERROR, SonarUiPlugin.PLUGIN_ID, Status.OK, "Unable to open Issues View", e));
              }
            }
          });
        }
      }
    });
    job.schedule();
  }

}
