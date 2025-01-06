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
package org.sonarlint.eclipse.ui.internal.binding.wizard.project;

import java.util.List;
import java.util.Objects;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration.EclipseProjectBinding;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.binding.actions.AnalysisJobsScheduler;

public class ProjectBindingProcess {
  public static boolean isProjectBound(ISonarLintProject project) {
    var config = SonarLintCorePlugin.loadConfig(project);
    return config.getProjectBinding().isPresent();
  }

  public static void bindProjects(String connectionId, List<ISonarLintProject> projects, String projectKey) {
    projects.forEach(p -> {
      var changed = false;
      var projectConfig = SonarLintCorePlugin.loadConfig(p);
      var oldBinding = projectConfig.getProjectBinding();
      var newBinding = new EclipseProjectBinding(connectionId, projectKey);
      if (oldBinding.isEmpty() || !Objects.equals(oldBinding.get(), newBinding)) {
        projectConfig.setProjectBinding(newBinding);
        changed = true;
      }
      if (changed) {
        SonarLintCorePlugin.saveConfig(p, projectConfig);
        p.deleteAllMarkers(SonarLintCorePlugin.MARKER_ON_THE_FLY_ID);
        p.deleteAllMarkers(SonarLintCorePlugin.MARKER_REPORT_ID);
        AnalysisJobsScheduler.notifyBindingViewAfterBindingChange(p, oldBinding.map(EclipseProjectBinding::getConnectionId).orElse(null));
      }
    });
  }

  private ProjectBindingProcess() {
    // utility class
  }
}
