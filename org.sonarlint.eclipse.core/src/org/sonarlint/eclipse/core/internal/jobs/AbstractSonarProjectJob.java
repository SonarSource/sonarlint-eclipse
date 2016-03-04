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
package org.sonarlint.eclipse.core.internal.jobs;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProject;

public abstract class AbstractSonarProjectJob extends WorkspaceJob {

  private final SonarLintProject sonarProject;

  public AbstractSonarProjectJob(String title, SonarLintProject project) {
    super(title);
    this.sonarProject = project;
    setPriority(Job.DECORATE);
    setRule(ResourcesPlugin.getWorkspace().getRuleFactory().markerRule(project.getProject()));
  }

  @Override
  public final IStatus runInWorkspace(final IProgressMonitor monitor) {
    return doRun(monitor);
  }

  protected SonarLintProject getSonarProject() {
    return sonarProject;
  }

  protected abstract IStatus doRun(final IProgressMonitor monitor);

}
