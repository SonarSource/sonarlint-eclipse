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
package org.sonarlint.eclipse.ui.internal.binding.assist;

import java.util.List;
import java.util.concurrent.CancellationException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.ui.progress.UIJob;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.binding.ProjectToBindSelectionDialog;
import org.sonarlint.eclipse.ui.internal.binding.wizard.project.ProjectBindingProcess;
import org.sonarlint.eclipse.ui.internal.util.DisplayUtils;

public class AssistBindingJob extends UIJob {

  private final String connectionId;
  private final String projectKey;
  @Nullable
  private ISonarLintProject project;

  public AssistBindingJob(String connectionId, String projectKey) {
    super("Assist creating SonarLint binding");
    // We don't want to have this job visible to the user, as there should be a dialog anyway
    setSystem(true);
    this.connectionId = connectionId;
    this.projectKey = projectKey;
  }

  @Override
  public IStatus runInUIThread(IProgressMonitor monitor) {
    DisplayUtils.bringToFront();
    this.project = bindProjectTo(connectionId, projectKey);
    return Status.OK_STATUS;
  }

  private static ISonarLintProject bindProjectTo(String connectionId, String projectKey) {
    var pickedProject = ProjectToBindSelectionDialog.pickProject(projectKey, connectionId);
    if (pickedProject == null) {
      throw new CancellationException();
    }
    ProjectBindingProcess.bindProjects(connectionId, List.of(pickedProject), projectKey);
    return pickedProject;
  }

  @Nullable
  public ISonarLintProject getProject() {
    return project;
  }

}
