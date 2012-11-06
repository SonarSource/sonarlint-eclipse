/*
 * Sonar Eclipse
 * Copyright (C) 2010-2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.internal.ui.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.sonar.ide.eclipse.core.jobs.AnalyseProjectJob;
import org.sonar.ide.eclipse.internal.core.resources.ProjectProperties;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AnalyseProjectAction implements IObjectActionDelegate {

  public AnalyseProjectAction() {
    super();
  }

  /**
   * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
   */
  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
  }

  /**
   * @see org.eclipse.ui.IActionDelegate#run(IAction)
   */
  public void run(IAction action) {
    for (IProject project : projects) {
      new AnalyseProjectJob(project).schedule();
    }
  }

  private List<IProject> projects = new ArrayList<IProject>();

  /**
   * @see org.eclipse.ui.IActionDelegate#selectionChanged(IAction, ISelection)
   */
  public void selectionChanged(IAction action, ISelection selection) {
    projects.clear();
    boolean actionEnabled = true;
    // get All selected Elements
    if (selection != null & selection instanceof IStructuredSelection) {
      IStructuredSelection strucSelection = (IStructuredSelection) selection;
      for (Iterator<Object> iterator = strucSelection.iterator(); iterator.hasNext();) {
        Object element = iterator.next();

        IProject project = (IProject) element;
        ProjectProperties projectProperties = ProjectProperties.getInstance(project);
        actionEnabled &= projectProperties.isAnalysedLocally();
        projects.add(project);
      }
      action.setEnabled(actionEnabled);
    }
    else {
      action.setEnabled(false);
    }

  }

}
