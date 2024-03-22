/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.job;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Shell;
import org.sonarlint.eclipse.core.internal.jobs.AbstractSonarJob;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

public class ShareProjectBindingJob extends AbstractSonarJob {
  private final Shell shell;
  private final ISonarLintProject project;

  public ShareProjectBindingJob(Shell shell, ISonarLintProject project) {
    super("Share project binding for project: " + project.getName());
    this.shell = shell;
    this.project = project;
  }

  @Override
  protected IStatus doRun(IProgressMonitor monitor) throws CoreException {
    // 1) Check if project is part of hierarchy and check if root-projects are available

    // 2) Based on information gathered provide different dialogs
    // i) project part of no hierarchy
    // - dialog with question / buttons to save to this project, learn more, or to cancel
    // ii) project part of one hierarchy (e.g. Maven) and this project is the root project
    // - behave like i)
    // iii) project part of one hierarchy (e.g. Maven) and this project is not the root project
    // - dialog with question / buttons to save to root project or this project, learn more, or to cancel
    // "root project" is greyed out with tooltip if not in workspace, user can cancel, import and run again
    // iv) project part of multiple hierarchies (e.g. Maven, Gradle, ...)
    // - dialog has to choose the hierarchy they want to use, or cancel
    // - when not canceled, behave like iii)

    return null;
  }
}
