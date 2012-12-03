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
package org.sonar.ide.eclipse.ui.internal.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.core.internal.jobs.AnalyseProjectJob;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.ui.internal.console.SonarConsole;
import org.sonar.ide.eclipse.ui.internal.views.ViolationsView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AnalyseProjectAction implements IObjectActionDelegate {

  private static final Logger LOG = LoggerFactory.getLogger(AnalyseProjectAction.class);

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
    boolean debugEnabled = SonarConsole.isDebugEnabled();
    for (IProject project : projects) {
      AnalyseProjectJob job = new AnalyseProjectJob(project, debugEnabled);
      // Display violation view after analysis is completed
      job.addJobChangeListener(new JobChangeAdapter() {
        @Override
        public void done(IJobChangeEvent event) {
          if (Status.OK_STATUS == event.getResult()) {
            Display.getDefault().asyncExec(new Runnable() {
              public void run() {
                IWorkbenchWindow iw = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                try {
                  iw.getActivePage().showView(ViolationsView.ID);
                } catch (PartInitException e) {
                  LOG.error("Unable to open Violation View", e);
                }
              }
            });
          }
        }
      });
      job.schedule();
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
    if (selection != null && selection instanceof IStructuredSelection) {
      IStructuredSelection strucSelection = (IStructuredSelection) selection;
      for (Iterator<Object> iterator = strucSelection.iterator(); iterator.hasNext();) {
        Object element = iterator.next();

        IProject project = (IProject) element;
        SonarProject projectProperties = SonarProject.getInstance(project);
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
