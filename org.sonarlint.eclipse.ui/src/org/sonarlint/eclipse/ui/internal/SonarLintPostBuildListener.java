/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.texteditor.ITextEditor;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest.FileWithDocument;
import org.sonarlint.eclipse.core.internal.utils.JobUtils;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.binding.actions.AnalysisJobsScheduler;
import org.sonarlint.eclipse.ui.internal.util.PlatformUtils;

/**
 * Responsible to trigger analysis when files are changed
 */
public class SonarLintPostBuildListener implements IResourceChangeListener {

  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    if (event.getType() == IResourceChangeEvent.POST_BUILD && event.getBuildKind() != IncrementalProjectBuilder.CLEAN_BUILD) {
      final var changedFiles = new ArrayList<ISonarLintFile>();
      try {
        event.getDelta().accept(delta -> visitDelta(changedFiles, delta));
      } catch (CoreException e) {
        SonarLintLogger.get().error(e.getMessage(), e);
      }

      if (!changedFiles.isEmpty()) {
        Map<ISonarLintProject, List<ISonarLintFile>> filesPerProject = changedFiles.stream()
          .collect(Collectors.groupingBy(ISonarLintFile::getProject, Collectors.toList()));

        SonarLintUiPlugin.removePostBuildListener();
        var job = new AnalyzeOpenedFiles(filesPerProject);
        JobUtils.scheduleAfter(job, SonarLintUiPlugin::addPostBuildListener);
        job.schedule();
      }
    }
  }

  private static class AnalyzeOpenedFiles extends Job {

    private final Map<ISonarLintProject, List<ISonarLintFile>> changedFilesPerProject;

    AnalyzeOpenedFiles(Map<ISonarLintProject, List<ISonarLintFile>> changedFilesPerProject) {
      super("Find opened files");
      this.changedFilesPerProject = changedFilesPerProject;
    }

    @Override
    public IStatus run(IProgressMonitor monitor) {
      for (var entry : changedFilesPerProject.entrySet()) {
        var project = entry.getKey();

        var filesToAnalyze = entry.getValue().stream()
          .map(f -> {
            var editorPart = PlatformUtils.findEditor(f);
            if (editorPart instanceof ITextEditor) {
              var textEditor = (ITextEditor) editorPart;
              var doc = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
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
          var request = new AnalyzeProjectRequest(project, filesToAnalyze, TriggerType.EDITOR_CHANGE, false);
          AnalysisJobsScheduler.scheduleAutoAnalysisIfEnabled(request);
        }
      }
      return Status.OK_STATUS;
    }
  }

  private static boolean visitDelta(final Collection<ISonarLintFile> changedFiles, IResourceDelta delta) {
    if (!SonarLintUtils.isSonarLintFileCandidate(delta.getResource())) {
      return false;
    }

    var resourceSonarLintProject = SonarLintUtils.adapt(delta.getResource().getProject(), ISonarLintProject.class,
      "[SonarLintPostBuildListener#visitDelta] Try get project of Eclipse project '"
        + delta.getResource().getProject() + "'");
    if (resourceSonarLintProject != null && !SonarLintUtils.isSonarLintFileCandidate(delta.getResource())) {
      return false;
    }

    var sonarLintProject = SonarLintUtils.adapt(delta.getResource(), ISonarLintProject.class,
      "[SonarLintPostBuildListener#visitDelta] Try get project of resource '" + delta.getResource() + "'");
    if (sonarLintProject != null) {
      return SonarLintCorePlugin.loadConfig(sonarLintProject).isAutoEnabled();
    }

    var sonarLintFile = SonarLintUtils.adapt(delta.getResource(), ISonarLintFile.class,
      "[SonarLintPostBuildListener#visitDelta] Try get file of resource '" + delta.getResource() + "'");
    if (sonarLintFile != null && (isChanged(delta) || isAdded(delta))) {
      changedFiles.add(sonarLintFile);
      return true;
    }

    return true;
  }

  private static boolean isChanged(IResourceDelta delta) {
    return (delta.getKind() & IResourceDelta.CHANGED) != 0 && (delta.getFlags() & IResourceDelta.CONTENT) != 0;
  }

  private static boolean isAdded(IResourceDelta delta) {
    return (delta.getKind() & IResourceDelta.ADDED) != 0;
  }

}
