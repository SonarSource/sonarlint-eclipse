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
package org.sonarlint.eclipse.ui.internal.command;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.ui.PlatformUI;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectionFacade;
import org.sonarlint.eclipse.ui.internal.SonarLintProjectDecorator;
import org.sonarlint.eclipse.ui.internal.binding.actions.AnalysisJobsScheduler;
import org.sonarlint.eclipse.ui.internal.util.SelectionUtils;

public class UnbindProjectsCommand extends AbstractHandler {

  @Nullable
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    var selectedProjects = SelectionUtils.allSelectedProjects(event, false);

    if (!selectedProjects.isEmpty()) {
      var job = new Job("Unbind projects") {

        @Override
        protected IStatus run(IProgressMonitor monitor) {
          monitor.beginTask("Unbind projects", selectedProjects.size());
          for (var p : selectedProjects) {
            var projectConfig = SonarLintCorePlugin.loadConfig(p);
            projectConfig.getProjectBinding().ifPresent(b -> {
              var oldServerId = b.getConnectionId();
              ConnectionFacade.unbind(p);
              AnalysisJobsScheduler.scheduleAnalysisOfOpenFiles(p, TriggerType.BINDING_CHANGE);
              AnalysisJobsScheduler.notifyBindingViewAfterBindingChange(p, oldServerId);
            });
            monitor.worked(1);
          }
          var labelProvider = PlatformUI.getWorkbench().getDecoratorManager().getBaseLabelProvider(SonarLintProjectDecorator.ID);
          if (labelProvider != null) {
            ((SonarLintProjectDecorator) labelProvider).fireChange(selectedProjects);
          }
          return Status.OK_STATUS;
        }
      };
      job.schedule();
    }

    return null;
  }

}
