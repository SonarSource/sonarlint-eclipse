/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2017 SonarSource SA
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.sonarlint.eclipse.core.internal.adapter.Adapters;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectJob;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest.FileWithDocument;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarlint.eclipse.ui.internal.util.PlatformUtils;
import org.sonarlint.eclipse.ui.internal.util.SelectionUtils;
import org.sonarlint.eclipse.ui.internal.views.issues.IssuesView;

public class AnalyzeCommand extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);
    final Map<ISonarLintProject, Collection<FileWithDocument>> filesPerProject;
    if (selection instanceof IStructuredSelection) {
      filesPerProject = findSelectedFilesPerProject(event);
    } else {
      filesPerProject = new HashMap<>(1);
      FileWithDocument editedFile = findEditedFile(event);
      if (editedFile != null) {
        filesPerProject.put(editedFile.getFile().getProject(), Arrays.asList(editedFile));
      }
    }

    if (!filesPerProject.isEmpty()) {
      runAnalysisJobs(filesPerProject);
    }

    return null;
  }

  private void runAnalysisJobs(Map<ISonarLintProject, Collection<FileWithDocument>> filesPerProject) {
    for (Map.Entry<ISonarLintProject, Collection<FileWithDocument>> entry : filesPerProject.entrySet()) {
      AnalyzeProjectJob job = new AnalyzeProjectJob(new AnalyzeProjectRequest(entry.getKey(), entry.getValue(), TriggerType.ACTION));
      showIssuesViewAfterJobSuccess(job);
    }
  }

  protected Map<ISonarLintProject, Collection<FileWithDocument>> findSelectedFilesPerProject(ExecutionEvent event) throws ExecutionException {
    Map<ISonarLintProject, Collection<FileWithDocument>> filesToAnalyzePerProject = new LinkedHashMap<>();
    for (ISonarLintFile file : SelectionUtils.allSelectedFiles(event)) {
      filesToAnalyzePerProject.putIfAbsent(file.getProject(), new ArrayList<FileWithDocument>());
      IEditorPart editorPart = PlatformUtils.findEditor(HandlerUtil.getActivePart(event).getSite().getPage(), file);
      if (editorPart instanceof ITextEditor) {
        IDocument doc = ((ITextEditor) editorPart).getDocumentProvider().getDocument(editorPart.getEditorInput());
        filesToAnalyzePerProject.get(file.getProject()).add(new FileWithDocument(file, doc));
      } else {
        filesToAnalyzePerProject.get(file.getProject()).add(new FileWithDocument(file, null));
      }
    }
    return filesToAnalyzePerProject;
  }

  static FileWithDocument findEditedFile(ExecutionEvent event) {
    IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
    if (activeEditor == null) {
      return null;
    }
    IEditorInput input = activeEditor.getEditorInput();
    if (input instanceof IFileEditorInput) {
      IDocument doc = ((ITextEditor) activeEditor).getDocumentProvider().getDocument(activeEditor.getEditorInput());
      IFile file = ((IFileEditorInput) input).getFile();
      ISonarLintFile sonarLintFile = Adapters.adapt(file, ISonarLintFile.class);
      return sonarLintFile != null ? new FileWithDocument(sonarLintFile, doc) : null;
    }
    return null;
  }

  protected void showIssuesViewAfterJobSuccess(Job job) {
    // Display issues view after analysis is completed
    job.addJobChangeListener(new JobChangeAdapter() {
      @Override
      public void done(IJobChangeEvent event) {
        if (Status.OK_STATUS == event.getResult()) {
          Display.getDefault().asyncExec(() -> {
            IWorkbenchWindow iw = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            try {
              iw.getActivePage().showView(IssuesView.ID, null, IWorkbenchPage.VIEW_VISIBLE);
            } catch (PartInitException e) {
              SonarLintUiPlugin.getDefault().getLog().log(new Status(Status.ERROR, SonarLintUiPlugin.PLUGIN_ID, Status.OK, "Unable to open Issues View", e));
            }
          });
        }
      }
    });
    job.schedule();
  }

}
