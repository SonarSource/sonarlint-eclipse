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
package org.sonarlint.eclipse.ui.internal.job;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.internal.resources.ProjectsProviderUtils;
import org.sonarlint.eclipse.core.internal.resources.ProjectsProviderUtils.WorkspaceProjectsBindingRatio;
import org.sonarlint.eclipse.core.internal.telemetry.LinkTelemetry;
import org.sonarlint.eclipse.ui.internal.util.MessageDialogUtils;

/** Job triggered when the rules configuration page changes are saved in order to not block the UI */
public class RulesConfigurationPageSaveJob extends Job {
  public RulesConfigurationPageSaveJob() {
    super("Saving the rules configuration");
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    var boundToAllProjectsRatio = ProjectsProviderUtils.boundToAllProjectsRatio();
    if (boundToAllProjectsRatio == WorkspaceProjectsBindingRatio.ALL_BOUND) {
      // all projects are bound, inform the user about local changes don't apply
      MessageDialogUtils.connectedModeOnlyInformation("Changing rule configuration has no effect",
        "As all the projects found in the workspace are already in Connected Mode, rule changes done locally "
          + "will have no impact on the analysis and its results. This has to be done on SonarQube (Server, Cloud).",
        LinkTelemetry.RULES_SELECTION_DOCS);
    } else if (!SonarLintGlobalConfiguration.ignoreEnhancedFeatureNotifications()) {
      MessageDialogUtils.enhancedWithConnectedModeInformation("Are you working in a team?",
        "When using Connected Mode you can benefit from having the rule configuration centralized. It is "
          + "synchronized to all project contributers using SonarQube for IDE, no manual configuration has to be done "
          + "locally.");
    }

    return Status.OK_STATUS;
  }
}
