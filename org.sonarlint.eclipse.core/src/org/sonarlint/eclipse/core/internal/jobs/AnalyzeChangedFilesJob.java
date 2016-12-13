/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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
import java.util.Map;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.sonarlint.eclipse.core.internal.SonarLintChangeListener;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;

public class AnalyzeChangedFilesJob extends WorkspaceJob {
  private static final String UNABLE_TO_ANALYZE_CHANGED_FILES = "Unable to analyze changed files";
  private final Collection<IProject> projects;

  public AnalyzeChangedFilesJob(Collection<IProject> projects) {
    super("Analyze changeset");
    this.projects = projects;
  }

  @Override
  public IStatus runInWorkspace(IProgressMonitor monitor) {
    SubMonitor global = SubMonitor.convert(monitor, 100);
    try {
      global.setTaskName("Collect changed file(s) list");
      Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects()).stream().forEach(MarkerUtils::deleteChangeSetIssuesMarkers);
      Collection<IFile> collectChangedFiles = collectChangedFiles(projects, global.newChild(20));

      if (collectChangedFiles.isEmpty()) {
        SonarLintCorePlugin.getDefault().info("No changed files found");
        return Status.OK_STATUS;
      }

      Map<IProject, Collection<IFile>> changedFilesPerProject = SonarLintUtils.aggregatePerMoreSpecificProject(collectChangedFiles);

      long fileCount = changedFilesPerProject.values().stream().flatMap(Collection::stream).count();

      SonarLintCorePlugin.getDefault().info("Analyzing " + fileCount + " changed file(s) in " + changedFilesPerProject.keySet().size() + " project(s)");

      global.setTaskName("Analysis");
      SubMonitor analysisMonitor = SubMonitor.convert(global.newChild(80), changedFilesPerProject.entrySet().size());
      for (Map.Entry<IProject, Collection<IFile>> entry : changedFilesPerProject.entrySet()) {
        SubMonitor projectAnalysisMonitor = analysisMonitor.newChild(1);
        IProject project = entry.getKey();
        global.setTaskName("Analysing project " + project.getName());
        if (!project.isAccessible()) {
          continue;
        }
        Collection<IFile> filesToAnalyze = entry.getValue();
        AnalyzeProjectRequest req = new AnalyzeProjectRequest(project, filesToAnalyze, TriggerType.CHANGESET);
        AnalyzeProjectJob job = new AnalyzeProjectJob(req);
        job.runInWorkspace(projectAnalysisMonitor);
      }

    } catch (Exception e) {
      SonarLintCorePlugin.getDefault().error(UNABLE_TO_ANALYZE_CHANGED_FILES, e);
      return new Status(Status.ERROR, SonarLintCorePlugin.PLUGIN_ID, UNABLE_TO_ANALYZE_CHANGED_FILES, e);
    }
    return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
  }

  private Collection<IFile> collectChangedFiles(Collection<IProject> projects, IProgressMonitor monitor) {
    Collection<IFile> changedFiles = new ArrayList<>();
    for (IProject project : projects) {
      if (monitor.isCanceled()) {
        break;
      }
      RepositoryProvider provider = RepositoryProvider.getProvider(project);
      if (provider == null) {
        SonarLintCorePlugin.getDefault().debug("Project " + project.getName() + " doesn't have any RepositoryProvider");
        continue;
      }

      Subscriber subscriber = provider.getSubscriber();
      if (subscriber == null) {
        // Seems to occurs with Rational ClearTeam Explorer
        SonarLintCorePlugin.getDefault().debug("No Subscriber for provider " + provider.getID() + " on project " + project.getName());
        continue;
      }

      try {
        IResource[] roots = subscriber.roots();
        if (Arrays.asList(roots).contains(project)) {
          // Refresh
          subscriber.refresh(new IResource[] {project}, IResource.DEPTH_INFINITE, monitor);

          // Collect all the synchronization states and print
          collect(subscriber, project, changedFiles);
        } else {
          SonarLintCorePlugin.getDefault().debug("Project " + project.getName() + " is not part of Subscriber roots");
        }
      } catch (TeamException e) {
        throw new IllegalStateException(UNABLE_TO_ANALYZE_CHANGED_FILES, e);
      }
    }
    return changedFiles;
  }

  void collect(Subscriber subscriber, IResource resource, Collection<IFile> changedFiles) throws TeamException {
    if (!SonarLintChangeListener.shouldAnalyze(resource)) {
      return;
    }
    IFile file = (IFile) resource.getAdapter(IFile.class);
    if (file != null) {
      SyncInfo syncInfo = subscriber.getSyncInfo(resource);
      if (syncInfo != null && !SyncInfo.isInSync(syncInfo.getKind())) {
        changedFiles.add(file);
      }
    }
    for (IResource child : subscriber.members(resource)) {
      collect(subscriber, child, changedFiles);
    }
  }
}
