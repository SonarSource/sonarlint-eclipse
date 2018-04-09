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
package org.sonarlint.eclipse.ui.internal.server.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.adapter.Adapters;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectJob;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest.FileWithDocument;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.SonarLintProjectDecorator;

public class JobUtils {

  private JobUtils() {
    // utility class, forbidden constructor
  }

  /**
   * Schedule analysis of open files of a project.
   * Use null for project parameter to analyze open files in all projects.
   */
  public static void scheduleAnalysisOfOpenFiles(@Nullable ISonarLintProject project, TriggerType triggerType, Predicate<ISonarLintFile> filter) {
    Map<ISonarLintProject, List<FileWithDocument>> filesByProject = new HashMap<>();

    collectOpenedFiles(project, filesByProject, filter);

    for (Map.Entry<ISonarLintProject, List<FileWithDocument>> entry : filesByProject.entrySet()) {
      ISonarLintProject aProject = entry.getKey();
      if (aProject.isOpen() && SonarLintProjectConfiguration.read(aProject.getScopeContext()).isAutoEnabled()) {
        AnalyzeProjectRequest request = new AnalyzeProjectRequest(aProject, entry.getValue(), triggerType);
        new AnalyzeProjectJob(request).schedule();
      }
    }
  }

  public static void scheduleAnalysisOfOpenFiles(@Nullable ISonarLintProject project, TriggerType triggerType) {
    scheduleAnalysisOfOpenFiles(project, triggerType, f -> true);
  }

  private static void collectOpenedFiles(@Nullable ISonarLintProject project, Map<ISonarLintProject, List<FileWithDocument>> filesByProject,
    Predicate<ISonarLintFile> filter) {
    for (IWorkbenchWindow win : PlatformUI.getWorkbench().getWorkbenchWindows()) {
      for (IWorkbenchPage page : win.getPages()) {
        for (IEditorReference ref : page.getEditorReferences()) {
          collectOpenedFile(project, filesByProject, ref, filter);
        }
      }
    }
  }

  private static void collectOpenedFile(@Nullable ISonarLintProject project, Map<ISonarLintProject, List<FileWithDocument>> filesByProject,
    IEditorReference ref, Predicate<ISonarLintFile> filter) {
    // Be careful to not trigger editor activation
    IEditorPart editor = ref.getEditor(false);
    if (editor == null) {
      return;
    }
    IEditorInput input = editor.getEditorInput();
    if (input instanceof IFileEditorInput) {
      IFile file = ((IFileEditorInput) input).getFile();
      ISonarLintFile sonarFile = Adapters.adapt(file, ISonarLintFile.class);
      if (sonarFile != null && (project == null || sonarFile.getProject().equals(project)) && filter.test(sonarFile)) {
        filesByProject.putIfAbsent(sonarFile.getProject(), new ArrayList<>());
        if (editor instanceof ITextEditor) {
          IDocument doc = ((ITextEditor) editor).getDocumentProvider().getDocument(input);
          filesByProject.get(sonarFile.getProject()).add(new FileWithDocument(sonarFile, doc));
        } else {
          filesByProject.get(sonarFile.getProject()).add(new FileWithDocument(sonarFile, null));
        }
      }
    }
  }

  public static void scheduleAnalysisOfOpenFiles(List<ISonarLintProject> projects, TriggerType triggerType) {
    projects.forEach(p -> scheduleAnalysisOfOpenFiles(p, triggerType));
  }

  public static void scheduleAnalysisOfOpenFilesInBoundProjects(IServer server, TriggerType triggerType) {
    scheduleAnalysisOfOpenFiles(server.getBoundProjects(), triggerType);
  }

  public static void scheduleAnalysisOfOpenFiles(Job job, List<ISonarLintProject> projects, TriggerType triggerType) {
    scheduleAfterSuccess(job, () -> scheduleAnalysisOfOpenFiles(projects, triggerType));
  }

  /**
   * Run something after the job is done, regardless of result.
   * Important: call job.schedule() after calling this method, NOT before.
   */
  public static void scheduleAfter(Job job, Runnable runnable) {
    job.addJobChangeListener(new JobCompletionListener() {
      @Override
      public void done(IJobChangeEvent event) {
        runnable.run();
      }
    });
  }

  /**
   * Run something after the job is done, with success. Do nothing if failed.
   * Important: call job.schedule() after calling this method, NOT before.
   */
  public static void scheduleAfterSuccess(Job job, Runnable runnable) {
    job.addJobChangeListener(new JobCompletionListener() {
      @Override
      public void done(IJobChangeEvent event) {
        if (event.getResult().isOK()) {
          runnable.run();
        }
      }
    });
  }

  public static void scheduleAnalysisOfOpenFilesInBoundProjects(Job job, IServer server, TriggerType triggerType) {
    scheduleAnalysisOfOpenFiles(job, server.getBoundProjects(), triggerType);
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

  public static void notifyServerViewAfterBindingChange(ISonarLintProject sonarProject, @Nullable String oldServerId) {
    SonarLintProjectConfiguration projectConfig = SonarLintProjectConfiguration.read(sonarProject.getScopeContext());
    String serverId = projectConfig.getServerId();
    if (oldServerId != null && !Objects.equals(serverId, oldServerId)) {
      IServer oldServer = SonarLintCorePlugin.getServersManager().getServer(oldServerId);
      if (oldServer != null) {
        oldServer.notifyAllListeners();
      }
    }
    if (serverId != null) {
      IServer server = SonarLintCorePlugin.getServersManager().getServer(serverId);
      if (server != null) {
        server.notifyAllListeners();
      }
    }
    IBaseLabelProvider labelProvider = PlatformUI.getWorkbench().getDecoratorManager().getBaseLabelProvider(SonarLintProjectDecorator.ID);
    if (labelProvider != null) {
      ((SonarLintProjectDecorator) labelProvider).fireChange(Arrays.asList(sonarProject));
    }
  }

}
