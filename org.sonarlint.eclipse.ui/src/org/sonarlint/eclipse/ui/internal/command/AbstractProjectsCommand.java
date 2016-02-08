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
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.handlers.HandlerUtil;

public abstract class AbstractProjectsCommand extends AbstractHandler {

  protected List<IProject> findSelectedProjects(ExecutionEvent event) throws ExecutionException {
    List<IProject> selectedProjects = new ArrayList<>();
    ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);

    if (selection instanceof IStructuredSelection) {
      Object[] elems = ((IStructuredSelection) selection).toArray();
      collectProjects(selectedProjects, elems);
    }

    return selectedProjects;
  }

  private void collectProjects(List<IProject> selectedProjects, Object[] elems) {
    for (Object elem : elems) {
      if (elem instanceof IProject) {
        selectedProjects.add((IProject) elem);
      } else if (elem instanceof IWorkingSet) {
        IWorkingSet ws = (IWorkingSet) elem;
        collectProjects(selectedProjects, ws.getElements());
      } else if (elem instanceof IAdaptable) {
        IProject proj = (IProject) ((IAdaptable) elem).getAdapter(IProject.class);
        if (proj != null) {
          selectedProjects.add(proj);
        }
      }
    }
  }

  protected static void findProjectOfSelectedEditor(ExecutionEvent event, List<IProject> selectedProjects) {
    IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
    IEditorInput input = activeEditor.getEditorInput();
    if (input instanceof IFileEditorInput) {
      IFile currentFile = ((IFileEditorInput) input).getFile();
      selectedProjects.add(currentFile.getProject());
    }
  }

}
