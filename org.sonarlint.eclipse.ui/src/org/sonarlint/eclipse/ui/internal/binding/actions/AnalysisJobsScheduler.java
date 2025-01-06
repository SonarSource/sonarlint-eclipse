/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.binding.actions;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.ui.PlatformUI;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectionFacade;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectJob;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration.EclipseProjectBinding;
import org.sonarlint.eclipse.core.internal.utils.JobUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.SonarLintProjectDecorator;
import org.sonarlint.eclipse.ui.internal.util.PlatformUtils;

public class AnalysisJobsScheduler {

  private AnalysisJobsScheduler() {
    // utility class, forbidden constructor
  }

  public static void scheduleAutoAnalysisIfEnabled(AnalyzeProjectRequest request) {
    var project = request.getProject();
    if (!project.isOpen()) {
      return;
    }
    var projectConfiguration = SonarLintCorePlugin.loadConfig(project);
    if (projectConfiguration.isAutoEnabled()) {
      AnalyzeProjectJob.create(request).schedule();
    }
  }

  /**
   * Schedule analysis of open files of a project.
   * Use null for project parameter to analyze open files in all projects.
   */
  public static void scheduleAnalysisOfOpenFiles(@Nullable ISonarLintProject project, TriggerType triggerType,
    Predicate<ISonarLintFile> filter) {
    var filesByProject = PlatformUtils.collectOpenedFiles(project, filter);

    for (var entry : filesByProject.entrySet()) {
      var aProject = entry.getKey();
      var request = new AnalyzeProjectRequest(aProject, entry.getValue(), triggerType, false);
      scheduleAutoAnalysisIfEnabled(request);
    }
  }

  public static void scheduleAnalysisOfOpenFiles(@Nullable ISonarLintProject project, TriggerType triggerType) {
    scheduleAnalysisOfOpenFiles(project, triggerType, f -> true);
  }

  public static void scheduleAnalysisOfOpenFiles(List<ISonarLintProject> projects, TriggerType triggerType) {
    projects.forEach(p -> scheduleAnalysisOfOpenFiles(p, triggerType, f -> true));
  }

  public static void scheduleAnalysisOfOpenFilesInBoundProjects(ConnectionFacade connection, TriggerType triggerType) {
    scheduleAnalysisOfOpenFiles(connection.getBoundProjects(), triggerType);
  }

  public static void scheduleAnalysisOfOpenFiles(Job job, List<ISonarLintProject> projects, TriggerType triggerType) {
    JobUtils.scheduleAfterSuccess(job, () -> scheduleAnalysisOfOpenFiles(projects, triggerType));
  }

  public static void scheduleAnalysisOfOpenFilesInBoundProjects(Job job, ConnectionFacade server, TriggerType triggerType) {
    scheduleAnalysisOfOpenFiles(job, server.getBoundProjects(), triggerType);
  }

  public static void notifyBindingViewAfterBindingChange(ISonarLintProject project, @Nullable String oldConnectionId) {
    var projectConfig = SonarLintCorePlugin.loadConfig(project);
    var connectionId = projectConfig.getProjectBinding().map(EclipseProjectBinding::getConnectionId).orElse(null);
    if (oldConnectionId != null && !Objects.equals(connectionId, oldConnectionId)) {
      var oldServer = SonarLintCorePlugin.getConnectionManager().findById(oldConnectionId);
      oldServer.ifPresent(ConnectionFacade::notifyAllListenersStateChanged);
    }
    if (connectionId != null) {
      var connection = SonarLintCorePlugin.getConnectionManager().findById(connectionId);
      connection.ifPresent(ConnectionFacade::notifyAllListenersStateChanged);
    }
    var labelProvider = PlatformUI.getWorkbench().getDecoratorManager().getBaseLabelProvider(SonarLintProjectDecorator.ID);
    if (labelProvider != null) {
      ((SonarLintProjectDecorator) labelProvider).fireChange(List.of(project));
    }
  }

}
