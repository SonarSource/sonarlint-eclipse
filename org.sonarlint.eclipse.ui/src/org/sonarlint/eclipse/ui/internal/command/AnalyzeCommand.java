/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2018 SonarSource SA
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
import java.util.Map.Entry;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.adapter.Adapters;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectJob;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest.FileWithDocument;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectsJob;
import org.sonarlint.eclipse.core.internal.utils.PreferencesUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.util.PlatformUtils;
import org.sonarlint.eclipse.ui.internal.util.SelectionUtils;

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
      runAnalysisJob(HandlerUtil.getActiveShell(event), filesPerProject);
    }

    return null;
  }

  private static void runAnalysisJob(Shell shell, Map<ISonarLintProject, Collection<FileWithDocument>> filesPerProject) {
    int totalFileCount = filesPerProject.values().stream().mapToInt(Collection::size).sum();
    if (totalFileCount > 1 && !askConfirmation(shell)) {
      return;
    }
    if (filesPerProject.size() == 1) {
      Entry<ISonarLintProject, Collection<FileWithDocument>> entry = filesPerProject.entrySet().iterator().next();
      AnalyzeProjectRequest req = new AnalyzeProjectRequest(entry.getKey(), entry.getValue(), TriggerType.MANUAL);
      int fileCount = req.getFilesToAnalyze().size();
      String reportTitle;
      if (fileCount == 1) {
        reportTitle = "File " + req.getFilesToAnalyze().iterator().next().getFile().getName();
      } else {
        reportTitle = fileCount + " files of project " + entry.getKey().getName();
      }
      AnalyzeProjectJob job = new AnalyzeProjectJob(req);
      AnalyzeChangeSetCommand.registerJobListener(job, reportTitle);
      job.schedule();
    } else {
      AnalyzeProjectsJob job = new AnalyzeProjectsJob(filesPerProject);
      AnalyzeChangeSetCommand.registerJobListener(job, "All files of " + filesPerProject.size() + " projects");
      job.schedule();
    }
  }

  private static boolean askConfirmation(Shell shell) {
    if (PreferencesUtils.skipConfirmAnalyzeMultipleFiles()) {
      return true;
    }

    // The general order is left to right, but the default button will be moved to the right.
    // http://www.vogella.com/tutorials/EclipseDialogs/article.html
    String[] buttonLabels = {
      "Proceed",
      IDialogConstants.CANCEL_LABEL,
      "Proceed and don't ask again",
    };

    MessageDialog dialog = new MessageDialog(shell, "Confirmation", null, "Analyzing multiple files may take a long time to complete.\n"
      + "To get the best from SonarLint, you should preferably use the on-the-fly analysis for the files you're working on.", MessageDialog.CONFIRM,
      buttonLabels, 0);

    switch (dialog.open()) {
      case 0:
        return true;
      case 2:
        PreferencesUtils.setSkipConfirmAnalyzeMultipleFiles();
        return true;
      default:
        return false;
    }
  }

  protected Map<ISonarLintProject, Collection<FileWithDocument>> findSelectedFilesPerProject(ExecutionEvent event) throws ExecutionException {
    Map<ISonarLintProject, Collection<FileWithDocument>> filesToAnalyzePerProject = new LinkedHashMap<>();
    for (ISonarLintFile file : SelectionUtils.allSelectedFiles(HandlerUtil.getCurrentSelectionChecked(event))) {
      filesToAnalyzePerProject.putIfAbsent(file.getProject(), new ArrayList<FileWithDocument>());
      IEditorPart editorPart = PlatformUtils.findEditor(file);
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

}
