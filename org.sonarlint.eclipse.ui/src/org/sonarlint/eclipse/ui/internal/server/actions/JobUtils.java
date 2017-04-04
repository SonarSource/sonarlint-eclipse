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
package org.sonarlint.eclipse.ui.internal.server.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.texteditor.ITextEditor;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.adapter.Adapters;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectJob;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest.FileWithDocument;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.core.internal.server.ServersManager;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.SonarLintProjectDecorator;

public class JobUtils {

  private JobUtils() {
    // utility class, forbidden constructor
  }

  // Make sure you only call this from within a Display, otherwise the workbench is not available
  // See: http://stackoverflow.com/questions/1265174/nullpointerexception-in-platformui-getworkbench-getactiveworkbenchwindow-get
  public static void scheduleAnalysisOfOpenFiles(@Nullable ISonarLintProject project, TriggerType triggerType) {
    Map<ISonarLintProject, List<FileWithDocument>> filesByProject = new HashMap<>();

    IWorkbenchWindow workbench = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if (workbench == null) {
      SonarLintLogger.get().debug("possible attempt to get workbench window outside of a Display");
      return;
    }

    IWorkbenchPage page = workbench.getActivePage();
    for (IEditorReference ref : page.getEditorReferences()) {
      try {
        IEditorInput input = ref.getEditorInput();
        collectOpenedFiles(project, filesByProject, page, input);
      } catch (PartInitException e) {
        SonarLintLogger.get().error("could not get editor content", e);
      }
    }

    for (Map.Entry<ISonarLintProject, List<FileWithDocument>> entry : filesByProject.entrySet()) {
      ISonarLintProject aProject = entry.getKey();
      if (aProject.isOpen() && SonarLintProjectConfiguration.read(aProject.getScopeContext()).isAutoEnabled()) {
        AnalyzeProjectRequest request = new AnalyzeProjectRequest(aProject, entry.getValue(), triggerType);
        new AnalyzeProjectJob(request).schedule();
      }
    }
  }

  private static void collectOpenedFiles(@Nullable ISonarLintProject project, Map<ISonarLintProject, List<FileWithDocument>> filesByProject, IWorkbenchPage page,
    IEditorInput input) {
    if (input instanceof IFileEditorInput) {
      IFile file = ((IFileEditorInput) input).getFile();
      ISonarLintFile sonarFile = Adapters.adapt(file, ISonarLintFile.class);
      if (sonarFile != null && (project == null || sonarFile.getProject().equals(project))) {
        filesByProject.putIfAbsent(sonarFile.getProject(), new ArrayList<>());
        IEditorPart editorPart = ResourceUtil.findEditor(page, file);
        if (editorPart instanceof ITextEditor) {
          IDocument doc = ((ITextEditor) editorPart).getDocumentProvider().getDocument(editorPart.getEditorInput());
          filesByProject.get(sonarFile.getProject()).add(new FileWithDocument(sonarFile, doc));
        } else {
          filesByProject.get(sonarFile.getProject()).add(new FileWithDocument(sonarFile, null));
        }
      }
    }
  }

  public static void scheduleAnalysisOfOpenFiles(List<ISonarLintProject> projects, TriggerType triggerType) {
    if (Display.getCurrent() != null) {
      projects.forEach(p -> scheduleAnalysisOfOpenFiles(p, triggerType));
    } else {
      Display.getDefault().asyncExec(() -> projects.forEach(p -> scheduleAnalysisOfOpenFiles(p, triggerType)));
    }
  }

  public static void scheduleAnalysisOfOpenFilesInBoundProjects(IServer server, TriggerType triggerType) {
    scheduleAnalysisOfOpenFiles(server.getBoundProjects(), triggerType);
  }

  public static void scheduleAnalysisOfOpenFiles(Job job, List<ISonarLintProject> projects, TriggerType triggerType) {
    job.addJobChangeListener(new JobCompletionListener() {
      @Override
      public void done(IJobChangeEvent event) {
        if (event.getResult().isOK()) {
          scheduleAnalysisOfOpenFiles(projects, triggerType);
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
      IServer oldServer = ServersManager.getInstance().getServer(oldServerId);
      if (oldServer != null) {
        oldServer.notifyAllListeners();
      }
    }
    if (serverId != null) {
      IServer server = ServersManager.getInstance().getServer(serverId);
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
