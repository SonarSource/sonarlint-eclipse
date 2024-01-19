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
package org.sonarlint.eclipse.ui.internal.binding.wizard.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.eclipse.core.runtime.jobs.Job;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.jobs.ProjectStorageUpdateJob;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration.EclipseProjectBinding;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.binding.actions.AnalysisJobsScheduler;

public class ProjectBindingProcess {

  public static Job scheduleProjectBinding(String serverId, List<ISonarLintProject> projects, String projectKey) {
    var job = new ProjectStorageUpdateJob(serverId, projectKey);
    var projectToSubscribeToNotifications = new ArrayList<ISonarLintProject>();
    projects.forEach(p -> {
      var changed = false;
      var projectConfig = SonarLintCorePlugin.loadConfig(p);
      var oldServerId = projectConfig.getProjectBinding().map(EclipseProjectBinding::connectionId).orElse(null);
      var oldProjectKey = projectConfig.getProjectBinding().map(EclipseProjectBinding::projectKey).orElse(null);
      if (!Objects.equals(serverId, oldServerId) || !Objects.equals(projectKey, oldProjectKey)) {
        // We can ignore path prefixes for now, they will be update by the ProjectStorageUpdateJob
        projectConfig.setProjectBinding(new EclipseProjectBinding(serverId, projectKey, "", ""));
        changed = true;
      }
      if (changed) {
        SonarLintCorePlugin.saveConfig(p, projectConfig);
        p.deleteAllMarkers(SonarLintCorePlugin.MARKER_ON_THE_FLY_ID);
        p.deleteAllMarkers(SonarLintCorePlugin.MARKER_REPORT_ID);
        SonarLintCorePlugin.clearIssueTracker(p);
        AnalysisJobsScheduler.notifyServerViewAfterBindingChange(p, oldServerId);
        projectToSubscribeToNotifications.add(p);
      }
    });

    AnalysisJobsScheduler.scheduleAnalysisOfOpenFiles(job, projects, TriggerType.BINDING_CHANGE, false);
    job.schedule();
    return job;
  }

  private ProjectBindingProcess() {
    // utility class
  }
}
