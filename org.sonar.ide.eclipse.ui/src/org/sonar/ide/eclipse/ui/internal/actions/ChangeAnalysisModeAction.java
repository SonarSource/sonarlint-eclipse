/*
 * Sonar Eclipse
 * Copyright (C) 2010-2013 SonarSource
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
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.sonar.ide.eclipse.core.internal.jobs.AnalyseProjectJob;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.ui.internal.SonarUiPlugin;
import org.sonar.ide.eclipse.ui.internal.console.SonarConsole;
import org.sonar.ide.eclipse.ui.internal.jobs.SynchronizeAllIssuesJob;
import org.sonar.ide.eclipse.ui.internal.util.SelectionUtils;

public class ChangeAnalysisModeAction implements IObjectActionDelegate {

  public static final String LOCAL_MODE = "org.sonar.ide.eclipse.runtime.ui.enableLocalAnalysisAction";
  public static final String REMOTE_MODE = "org.sonar.ide.eclipse.runtime.ui.enableRemoteAnalysisAction";

  /**
   * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
   */
  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
  }

  /**
   * @see org.eclipse.ui.IActionDelegate#run(IAction)
   */
  public void run(IAction action) {
    SonarProject projectProperties = SonarProject.getInstance(project);
    projectProperties.setAnalysedLocally(isLocalAnalysis(action));
    projectProperties.setLastAnalysisDate(null);
    projectProperties.save();

    if (isLocalAnalysis(action)) {
      new AnalyseProjectJob(project, SonarConsole.isDebugEnabled(), false,
        SonarUiPlugin.getExtraPropertiesForLocalAnalysis(project), SonarUiPlugin.getSonarJvmArgs()).schedule();
    } else {
      SynchronizeAllIssuesJob.createAndSchedule(project);
    }
  }

  private IProject project;

  /**
   * @see org.eclipse.ui.IActionDelegate#selectionChanged(IAction, ISelection)
   */
  public void selectionChanged(IAction action, ISelection selection) {
    project = (IProject) SelectionUtils.getSingleElement(selection);
    if (project != null) {
      SonarProject projectProperties = SonarProject.getInstance(project);
      action.setChecked(isChecked(action, projectProperties));
      action.setEnabled(!action.isChecked());
    }
  }

  private boolean isChecked(IAction action, SonarProject projectProperties) {
    if (isLocalAnalysis(action)) {
      return projectProperties.isAnalysedLocally();
    } else {
      return !projectProperties.isAnalysedLocally();
    }
  }

  private boolean isLocalAnalysis(IAction action) {
    return LOCAL_MODE.equals(action.getId());
  }
}
