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
package org.sonarlint.eclipse.ui.internal.command;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProject;
import org.sonarlint.eclipse.ui.internal.SonarLintProjectDecorator;

public class UnbindProjectsCommand extends AbstractHandler {

  public Display getDisplay() {
    Display display = Display.getCurrent();
    if (display == null) {
      display = Display.getDefault();
    }
    return display;
  }

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    IStructuredSelection selection = (IStructuredSelection) HandlerUtil.getCurrentSelectionChecked(event);

    final List<IProject> selectedProjects = new ArrayList<>();

    @SuppressWarnings("rawtypes")
    List elems = selection.toList();
    for (Object elem : elems) {
      if (elem instanceof IProject) {
        selectedProjects.add((IProject) elem);
      } else if (elem instanceof IAdaptable) {
        IProject proj = (IProject) ((IAdaptable) elem).getAdapter(IProject.class);
        if (proj != null) {
          selectedProjects.add(proj);
        }
      }
    }

    Job job = new Job("Unbind projects") {

      @Override
      protected IStatus run(IProgressMonitor monitor) {
        monitor.beginTask("Unbind projects", selectedProjects.size());
        for (IProject p : selectedProjects) {
          SonarLintProject sonarLintProject = SonarLintProject.getInstance(p);
          sonarLintProject.setServerId(null);
          sonarLintProject.setModuleKey(null);
          sonarLintProject.save();
          MarkerUtils.deleteIssuesMarkers(p);
          monitor.worked(1);
        }
        IBaseLabelProvider labelProvider = PlatformUI.getWorkbench().getDecoratorManager().getBaseLabelProvider(SonarLintProjectDecorator.ID);
        if (labelProvider != null) {
          ((SonarLintProjectDecorator) labelProvider).fireChange(selectedProjects.toArray(new IProject[0]));
        }
        return Status.OK_STATUS;
      }
    };
    job.schedule();

    return null;
  }

}
