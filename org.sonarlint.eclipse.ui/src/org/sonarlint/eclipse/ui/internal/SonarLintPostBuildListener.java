/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2020 SonarSource SA
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
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.adapter.Adapters;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest.FileWithDocument;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.binding.actions.JobUtils;
import org.sonarlint.eclipse.ui.internal.util.PlatformUtils;

/**
 * Responsible to trigger analysis when files are changed
 */
public class SonarLintPostBuildListener implements IResourceChangeListener {

  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    if (event.getType() == IResourceChangeEvent.POST_BUILD) {
      final Collection<ISonarLintFile> changedFiles = new ArrayList<>();
      try {
        event.getDelta().accept(delta -> visitDelta(changedFiles, delta));
      } catch (CoreException e) {
        SonarLintLogger.get().error(e.getMessage(), e);
      }

      if (!changedFiles.isEmpty()) {
        Map<ISonarLintProject, Collection<ISonarLintFile>> filesPerProject = changedFiles.stream()
          .collect(Collectors.groupingBy(ISonarLintFile::getProject, Collectors.toCollection(ArrayList::new)));

        SonarLintUiPlugin.removePostBuildListener();
        Job job = new AnalyzeOpenedFiles(filesPerProject);
        JobUtils.scheduleAfter(job, SonarLintUiPlugin::addPostBuildListener);
        job.schedule();
      }
    }
  }

  private static class AnalyzeOpenedFiles extends Job {

    private final Map<ISonarLintProject, Collection<ISonarLintFile>> changedFilesPerProject;

    AnalyzeOpenedFiles(Map<ISonarLintProject, Collection<ISonarLintFile>> changedFilesPerProject) {
      super("Find opened files");
      this.changedFilesPerProject = changedFilesPerProject;
    }

    @Override
    public IStatus run(IProgressMonitor monitor) {
      for (Map.Entry<ISonarLintProject, Collection<ISonarLintFile>> entry : changedFilesPerProject.entrySet()) {
        ISonarLintProject project = entry.getKey();

        Collection<FileWithDocument> filesToAnalyze = entry.getValue().stream()
          .map(f -> {
            IEditorPart editorPart = PlatformUtils.findEditor(f);
            if (editorPart instanceof ITextEditor) {
              ITextEditor textEditor = (ITextEditor) editorPart;
              IDocument doc = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
              return new FileWithDocument(f, doc);
            }
            if (editorPart != null) {
              // File is open in an editor, but we don't know how to get the IDocument
              return new FileWithDocument(f, null);
            }
            return null;
          })
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
        if (!filesToAnalyze.isEmpty()) {
          AnalyzeProjectRequest request = new AnalyzeProjectRequest(project, filesToAnalyze, TriggerType.EDITOR_CHANGE);
          JobUtils.scheduleAutoAnalysisIfEnabled(request);
        }
      }
      return Status.OK_STATUS;
    }
  }

  private static boolean visitDelta(final Collection<ISonarLintFile> changedFiles, IResourceDelta delta) {
    if (!SonarLintUtils.isSonarLintFileCandidate(delta.getResource())) {
      return false;
    }

    ISonarLintProject resourceSonarLintProject = Adapters.adapt(delta.getResource().getProject(), ISonarLintProject.class);
    if (resourceSonarLintProject != null && !SonarLintUtils.isSonarLintFileCandidate(delta.getResource())) {
      return false;
    }

    ISonarLintProject sonarLintProject = Adapters.adapt(delta.getResource(), ISonarLintProject.class);
    if (sonarLintProject != null) {
      return SonarLintCorePlugin.loadConfig(sonarLintProject).isAutoEnabled();
    }

    ISonarLintFile sonarLintFile = Adapters.adapt(delta.getResource(), ISonarLintFile.class);
    if (sonarLintFile != null && isChanged(delta)) {
      changedFiles.add(sonarLintFile);
      return true;
    }

    return true;
  }

  private static boolean isChanged(IResourceDelta delta) {
    return delta.getKind() == IResourceDelta.CHANGED && (delta.getFlags() & IResourceDelta.CONTENT) != 0;
  }

}
