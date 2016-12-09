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
import java.util.List;
import java.util.Map;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;

public class AnalyzeChangedFilesJob extends Job {
  private static final String UNABLE_TO_ANALYZE_CHANGED_FILES = "Unable to analyze changed files";
  private final Collection<IProject> projects;

  public AnalyzeChangedFilesJob(Collection<IProject> projects) {
    super("Analyze changeset");
    this.projects = projects;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    try {
      Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects()).stream().forEach(MarkerUtils::deleteChangeSetIssuesMarkers);
      Collection<IFile> collectChangedFiles = collectChangedFiles(projects, monitor);

      Map<IProject, Collection<IFile>> changedFilesPerProject = SonarLintUtils.aggregatePerMoreSpecificProject(collectChangedFiles);

      List<Job> jobs = new ArrayList<>();
      for (Map.Entry<IProject, Collection<IFile>> entry : changedFilesPerProject.entrySet()) {
        IProject project = entry.getKey();
        if (!project.isAccessible()) {
          continue;
        }
        Collection<IFile> filesToAnalyze = entry.getValue();
        AnalyzeProjectRequest req = new AnalyzeProjectRequest(project, filesToAnalyze, TriggerType.CHANGESET);
        AnalyzeProjectJob job = new AnalyzeProjectJob(req);
        job.schedule();
        jobs.add(job);
      }

      waitForAllJobsToComplete(jobs);
    } catch (Exception e) {
      SonarLintCorePlugin.getDefault().error(UNABLE_TO_ANALYZE_CHANGED_FILES, e);
      return new Status(Status.ERROR, SonarLintCorePlugin.PLUGIN_ID, UNABLE_TO_ANALYZE_CHANGED_FILES, e);
    }
    return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
  }

  private static void waitForAllJobsToComplete(List<Job> jobs) {
    jobs.stream().forEach(j -> {
      try {
        j.join();
      } catch (InterruptedException e) {
        // Ignore
      }
    });
  }

  private Collection<IFile> collectChangedFiles(Collection<IProject> projects, IProgressMonitor monitor) {
    Collection<IFile> changedFiles = new ArrayList<>();
    for (IProject project : projects) {
      if (monitor.isCanceled()) {
        break;
      }
      RepositoryProvider provider = RepositoryProvider.getProvider(project);
      if (provider == null) {
        continue;
      }

      Subscriber subscriber = provider.getSubscriber();

      // Allow the subscriber to refresh its state
      try {
        subscriber.refresh(new IResource[] {project}, IResource.DEPTH_INFINITE, monitor);

        // Collect all the synchronization states and print
        IResource[] children = new IResource[] {project};
        for (int i = 0; i < children.length; i++) {
          collect(subscriber, children[i], changedFiles);
        }
      } catch (TeamException e) {
        throw new IllegalStateException(UNABLE_TO_ANALYZE_CHANGED_FILES, e);
      }
    }
    return changedFiles;
  }

  void collect(Subscriber subscriber, IResource resource, Collection<IFile> changedFiles) throws TeamException {
    IFile file = (IFile) resource.getAdapter(IFile.class);
    if (file != null) {
      SyncInfo syncInfo = subscriber.getSyncInfo(resource);
      if (!SyncInfo.isInSync(syncInfo.getKind())) {
        changedFiles.add(file);
      }
    }
    for (IResource child : subscriber.members(resource)) {
      collect(subscriber, child, changedFiles);
    }
  }
}
