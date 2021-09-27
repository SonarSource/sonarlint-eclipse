/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2021 SonarSource SA
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

import java.util.Collection;
import java.util.Map;
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
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

public class AnalyzeProjectsJob extends WorkspaceJob {
  private static final String UNABLE_TO_ANALYZE_FILES = "Unable to analyze files";
  private final Map<ISonarLintProject, Collection<FileWithDocument>> filesPerProject;

  public AnalyzeProjectsJob(Map<ISonarLintProject, Collection<FileWithDocument>> filesPerProject) {
    super("Analyze all files");
    this.filesPerProject = filesPerProject;
    setPriority(Job.LONG);
  }

  @Override
  public IStatus runInWorkspace(IProgressMonitor monitor) {
    SubMonitor global = SubMonitor.convert(monitor, 100);
    try {
      global.setTaskName("Analysis");
      SonarLintMarkerUpdater.deleteAllMarkersFromReport();
      SubMonitor analysisMonitor = SubMonitor.convert(global.newChild(100), filesPerProject.size());
      for (Map.Entry<ISonarLintProject, Collection<FileWithDocument>> entry : filesPerProject.entrySet()) {
        if (monitor.isCanceled()) {
          return Status.CANCEL_STATUS;
        }
        ISonarLintProject project = entry.getKey();
        if (!project.isOpen()) {
          analysisMonitor.worked(1);
          continue;
        }
        global.setTaskName("Analyzing project " + project.getName());
        AnalyzeProjectRequest req = new AnalyzeProjectRequest(project, entry.getValue(), TriggerType.MANUAL);
        AbstractSonarProjectJob job = AbstractAnalyzeProjectJob.create(req);
        SubMonitor subMonitor = analysisMonitor.newChild(1);
        job.run(subMonitor);
        subMonitor.done();
      }

    } catch (Exception e) {
      SonarLintLogger.get().error(UNABLE_TO_ANALYZE_FILES, e);
      return new Status(Status.ERROR, SonarLintCorePlugin.PLUGIN_ID, UNABLE_TO_ANALYZE_FILES, e);
    }
    return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
  }

  @Override
  public final boolean belongsTo(Object family) {
    return "org.sonarlint.eclipse.projectsJob".equals(family);
  }

}
