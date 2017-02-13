/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2017 SonarSource SA
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.handlers.HandlerUtil;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest.FileWithDocument;
import org.sonarlint.eclipse.ui.internal.views.issues.ChangeSetIssuesView;

public class AnalyzeChangeSetCommand extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);

    final Set<IProject> selectedProjects = new HashSet<>();

    if (selection instanceof IStructuredSelection) {
      @SuppressWarnings("rawtypes")
      List elems = ((IStructuredSelection) selection).toList();
      for (Object elem : elems) {
        if (elem instanceof IWorkingSet) {
          for (IAdaptable elt : ((IWorkingSet) elem).getElements()) {
            collectProject(selectedProjects, elt);
          }
        } else if (elem instanceof IResource) {
          selectedProjects.add(((IResource) elem).getProject());
        } else if (elem instanceof IAdaptable) {
          collectProject(selectedProjects, elem);
        }
      }
    } else {
      FileWithDocument editedFile = AnalyzeCommand.findEditedFile(event);
      if (editedFile != null) {
        selectedProjects.add(editedFile.getFile().getProject());
      }
    }

    ChangeSetIssuesView.triggerAnalysis(selectedProjects);

    return null;
  }

  private static void collectProject(final Set<IProject> selectedProjects, Object elem) {
    IResource res = (IResource) ((IAdaptable) elem).getAdapter(IResource.class);
    if (res != null) {
      selectedProjects.add(res.getProject());
    }
  }

}
