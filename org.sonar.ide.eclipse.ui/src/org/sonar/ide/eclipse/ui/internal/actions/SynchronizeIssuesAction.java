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
import org.sonar.ide.eclipse.core.internal.jobs.AnalyseProjectRequest;
import org.sonar.ide.eclipse.core.internal.jobs.SynchronizeAllIssuesJob;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.ui.internal.SonarUiPlugin;
import org.sonar.ide.eclipse.ui.internal.console.SonarConsole;

import java.util.ArrayList;
import java.util.List;

public class SynchronizeIssuesAction extends AbstractSonarProjectAction {

  public SynchronizeIssuesAction() {
    super();
  }

  /**
   * @see org.eclipse.ui.IActionDelegate#run(IAction)
   */
  public void run(IAction action) {
    boolean debugEnabled = SonarConsole.isDebugEnabled();
    String sonarJvmArgs = SonarUiPlugin.getSonarJvmArgs();
    List<AnalyseProjectRequest> requests = new ArrayList<AnalyseProjectRequest>();
    for (IProject project : getSelectedProjects()) {
      requests.add(new AnalyseProjectRequest(project)
        .setDebugEnabled(debugEnabled)
        .setExtraProps(SonarUiPlugin.getExtraPropertiesForLocalAnalysis(project))
        .setJvmArgs(sonarJvmArgs));
    }
    SynchronizeAllIssuesJob job = new SynchronizeAllIssuesJob(requests);
    showIssuesViewAfterJobSuccess(job);
  }

  @Override
  protected boolean actionEnabled(SonarProject projectProperties) {
    return true;
  }

}
