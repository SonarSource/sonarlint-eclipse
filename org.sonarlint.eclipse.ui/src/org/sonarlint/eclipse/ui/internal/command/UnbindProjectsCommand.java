/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2018 SonarSource SA
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

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.adapter.Adapters;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.internal.server.Server;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.SonarLintProjectDecorator;
import org.sonarlint.eclipse.ui.internal.server.actions.JobUtils;

public class UnbindProjectsCommand extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    IStructuredSelection selection = (IStructuredSelection) HandlerUtil.getCurrentSelectionChecked(event);

    final List<ISonarLintProject> selectedProjects = new ArrayList<>();

    @SuppressWarnings("rawtypes")
    List elems = selection.toList();
    for (Object elem : elems) {
      ISonarLintProject proj = Adapters.adapt(elem, ISonarLintProject.class);
      if (proj != null) {
        selectedProjects.add(proj);
      }
    }

    Job job = new Job("Unbind projects") {

      @Override
      protected IStatus run(IProgressMonitor monitor) {
        monitor.beginTask("Unbind projects", selectedProjects.size());
        for (ISonarLintProject p : selectedProjects) {
          SonarLintProjectConfiguration projectConfig = SonarLintProjectConfiguration.read(p.getScopeContext());
          String oldServerId = projectConfig.getServerId();
          Server.unbind(p);
          JobUtils.scheduleAnalysisOfOpenFiles(p, TriggerType.BINDING_CHANGE);
          JobUtils.notifyServerViewAfterBindingChange(p, oldServerId);
          monitor.worked(1);
        }
        IBaseLabelProvider labelProvider = PlatformUI.getWorkbench().getDecoratorManager().getBaseLabelProvider(SonarLintProjectDecorator.ID);
        if (labelProvider != null) {
          ((SonarLintProjectDecorator) labelProvider).fireChange(selectedProjects);
        }
        return Status.OK_STATUS;
      }
    };
    job.schedule();

    return null;
  }

}
