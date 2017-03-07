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
package org.sonarlint.eclipse.ui.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.texteditor.ITextEditor;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectJob;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest.FileWithDocument;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.markers.ShowIssueFlowsMarkerResolver;

public class SonarLintChangeListener implements IResourceChangeListener {

  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
      final Collection<ISonarLintFile> changedFiles = new ArrayList<>();
      try {
        event.getDelta().accept(delta -> visitDelta(changedFiles, delta));
      } catch (CoreException e) {
        SonarLintLogger.get().error(e.getMessage(), e);
      }

      if (!changedFiles.isEmpty()) {
        new AnalyzeOpenedFiles(changedFiles.stream().collect(Collectors.groupingBy(ISonarLintFile::getProject, Collectors.toList()))).schedule();
      }
    }
  }

  private static class AnalyzeOpenedFiles extends UIJob {

    private final Map<ISonarLintProject, List<ISonarLintFile>> changedFilesPerProject;

    AnalyzeOpenedFiles(Map<ISonarLintProject, List<ISonarLintFile>> changedFilesPerProject) {
      super("Find opened files");
      this.changedFilesPerProject = changedFilesPerProject;
    }

    @Override
    public IStatus runInUIThread(IProgressMonitor monitor) {
      IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
      if (win == null) {
        return Status.OK_STATUS;
      }
      IWorkbenchPage page = win.getActivePage();
      if (page == null) {
        return Status.OK_STATUS;
      }
      for (Map.Entry<ISonarLintProject, List<ISonarLintFile>> entry : changedFilesPerProject.entrySet()) {
        ISonarLintProject project = entry.getKey();
        Collection<FileWithDocument> filesToAnalyze = entry.getValue().stream()
          .map(f -> {
            IEditorPart editorPart = ResourceUtil.findEditor(page, f.getFileInEditor());
            if (editorPart instanceof ITextEditor) {
              ITextEditor textEditor = (ITextEditor) editorPart;
              ShowIssueFlowsMarkerResolver.removeAnnotations(textEditor);
              IDocument doc = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
              return new FileWithDocument(f, doc);
            }
            return null;
          })
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
        AnalyzeProjectRequest request = new AnalyzeProjectRequest(project, filesToAnalyze, TriggerType.EDITOR_CHANGE);
        new AnalyzeProjectJob(request).schedule();
      }
      return Status.OK_STATUS;
    }
  }

  private static boolean visitDelta(final Collection<ISonarLintFile> changedFiles, IResourceDelta delta) {
    if (!SonarLintUtils.shouldAnalyze(delta.getResource())) {
      return false;
    }
    ISonarLintProject sonarLintProject = (ISonarLintProject) delta.getResource().getAdapter(ISonarLintProject.class);
    if (sonarLintProject != null) {
      return sonarLintProject.isAutoEnabled();
    }
    ISonarLintFile sonarLintFile = (ISonarLintFile) delta.getResource().getAdapter(ISonarLintFile.class);
    if (sonarLintFile != null && sonarLintFile.getProject().isAutoEnabled() && isChanged(delta)) {
      changedFiles.add(sonarLintFile);
      return true;
    }
    return true;
  }

  private static boolean isChanged(IResourceDelta delta) {
    return delta.getKind() == IResourceDelta.CHANGED
      && (delta.getFlags() & IResourceDelta.CONTENT) != 0;
  }

}
