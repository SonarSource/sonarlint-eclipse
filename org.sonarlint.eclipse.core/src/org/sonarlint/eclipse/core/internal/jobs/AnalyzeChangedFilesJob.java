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
package org.sonarlint.eclipse.core.internal.jobs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest.FileWithDocument;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

public class AnalyzeChangedFilesJob extends WorkspaceJob {
  private static final String UNABLE_TO_ANALYZE_CHANGED_FILES = "Unable to analyze changed files";
  private final Collection<ISonarLintProject> projects;

  public AnalyzeChangedFilesJob(Collection<ISonarLintProject> projects) {
    super("Analyze changeset");
    this.projects = projects;
  }

  @Override
  public IStatus runInWorkspace(IProgressMonitor monitor) {
    SubMonitor global = SubMonitor.convert(monitor, 100);
    try {
      global.setTaskName("Collect changed file(s) list");
      Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects()).stream().forEach(MarkerUtils::deleteChangeSetIssuesMarkers);
      Collection<ISonarLintFile> collectChangedFiles = collectChangedFiles(projects, global.newChild(20));

      if (collectChangedFiles.isEmpty()) {
        SonarLintLogger.get().info("No changed files found");
        return Status.OK_STATUS;
      }

      // FIXME duplicate Map<ISonarLintProject, Collection<ISonarLintFile>> changedFilesPerProject =
      // SonarLintUtils.aggregatePerMoreSpecificProject(collectChangedFiles);
      Map<ISonarLintProject, List<ISonarLintFile>> changedFilesPerProject = collectChangedFiles.stream().collect(Collectors.groupingBy(ISonarLintFile::getProject));

      long fileCount = changedFilesPerProject.values().stream().flatMap(Collection::stream).count();

      SonarLintLogger.get().info("Analyzing " + fileCount + " changed file(s) in " + changedFilesPerProject.keySet().size() + " project(s)");

      global.setTaskName("Analysis");
      SubMonitor analysisMonitor = SubMonitor.convert(global.newChild(80), changedFilesPerProject.entrySet().size());
      for (Map.Entry<ISonarLintProject, List<ISonarLintFile>> entry : changedFilesPerProject.entrySet()) {
        SubMonitor projectAnalysisMonitor = analysisMonitor.newChild(1);
        ISonarLintProject project = entry.getKey();
        if (!project.isOpen()) {
          continue;
        }
        global.setTaskName("Analysing project " + project.getName());
        Collection<FileWithDocument> filesToAnalyze = entry.getValue().stream()
          .map(f -> new FileWithDocument(f, null))
          .collect(Collectors.toList());
        AnalyzeProjectRequest req = new AnalyzeProjectRequest(project, filesToAnalyze, TriggerType.CHANGESET);
        AnalyzeProjectJob job = new AnalyzeProjectJob(req);
        job.runInWorkspace(projectAnalysisMonitor);
      }

    } catch (Exception e) {
      SonarLintLogger.get().error(UNABLE_TO_ANALYZE_CHANGED_FILES, e);
      return new Status(Status.ERROR, SonarLintCorePlugin.PLUGIN_ID, UNABLE_TO_ANALYZE_CHANGED_FILES, e);
    }
    return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
  }

  private static Collection<ISonarLintFile> collectChangedFiles(Collection<ISonarLintProject> projects, IProgressMonitor monitor) {
    Collection<ISonarLintFile> changedFiles = new ArrayList<>();
    for (ISonarLintProject project : projects) {
      if (monitor.isCanceled()) {
        break;
      }
      changedFiles.addAll(project.getScmChangedFiles(monitor));
    }
    return changedFiles;
  }

}
