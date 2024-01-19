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
package org.sonarlint.eclipse.core.internal.jobs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest.FileWithDocument;
import org.sonarlint.eclipse.core.internal.resources.DefaultSonarLintProjectAdapter;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

public class AnalyzeChangedFilesJob extends WorkspaceJob {
  private static final String UNABLE_TO_ANALYZE_CHANGED_FILES = "Unable to analyze changed files";
  private final Collection<ISonarLintProject> projects;

  public AnalyzeChangedFilesJob(Collection<ISonarLintProject> projects) {
    super("Analyze changed files");
    this.projects = projects;
    setPriority(Job.LONG);
  }

  @Override
  public IStatus runInWorkspace(IProgressMonitor monitor) {
    var global = SubMonitor.convert(monitor, 100);
    try {
      global.setTaskName("Collect changed file(s) list");
      SonarLintMarkerUpdater.deleteAllMarkersFromReport();
      var collectChangedFiles = collectChangedFiles(projects, global.newChild(20));

      if (collectChangedFiles.isEmpty()) {
        SonarLintLogger.get().info("No changed files found");
        return Status.OK_STATUS;
      }

      var changedFilesPerProject = collectChangedFiles.stream().collect(Collectors.groupingBy(ISonarLintFile::getProject));

      long fileCount = changedFilesPerProject.values().stream().flatMap(Collection::stream).count();

      SonarLintLogger.get().info("Analyzing " + fileCount + " changed file(s) in " + changedFilesPerProject.size() + " project(s)");

      global.setTaskName("Analysis");
      var analysisMonitor = SubMonitor.convert(global.newChild(80), changedFilesPerProject.size());
      for (var entry : changedFilesPerProject.entrySet()) {
        if (monitor.isCanceled()) {
          return Status.CANCEL_STATUS;
        }
        var project = entry.getKey();
        if (!project.isOpen()) {
          analysisMonitor.worked(1);
          continue;
        }
        global.setTaskName("Analyzing project " + project.getName());
        var filesToAnalyze = entry.getValue().stream()
          .map(f -> new FileWithDocument(f, null))
          .collect(Collectors.toList());
        
        // If the project is bound, we don't have to check for unsupported languages.
        var req = new AnalyzeProjectRequest(project, filesToAnalyze, TriggerType.MANUAL_CHANGESET, false,
          !SonarLintUtils.isBoundToConnection(project));
        
        var job = AbstractAnalyzeProjectJob.create(req);
        var subMonitor = analysisMonitor.newChild(1);
        job.run(subMonitor);
        subMonitor.done();
      }

    } catch (Exception e) {
      SonarLintLogger.get().error(UNABLE_TO_ANALYZE_CHANGED_FILES, e);
      return new Status(Status.ERROR, SonarLintCorePlugin.PLUGIN_ID, UNABLE_TO_ANALYZE_CHANGED_FILES, e);
    }
    return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
  }

  private static Collection<ISonarLintFile> collectChangedFiles(Collection<ISonarLintProject> projects, IProgressMonitor monitor) {
    var changedFiles = new ArrayList<ISonarLintFile>();
    for (var project : projects) {
      if (monitor.isCanceled()) {
        break;
      }
      if (project instanceof DefaultSonarLintProjectAdapter) {
        changedFiles.addAll(((DefaultSonarLintProjectAdapter) project).getScmChangedFiles(monitor));
      }
    }
    return changedFiles;
  }

}
