/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2023 SonarSource SA
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;

/** Base class for all SonarLint jobs, for level specific jobs see subclasses */
public abstract class AbstractSonarJob extends Job {
  public AbstractSonarJob(String title) {
    super(title);
    setPriority(Job.DECORATE);
  }

  @Override
  public final IStatus run(final IProgressMonitor monitor) {
    try {
      return doRun(monitor);
    } catch (CoreException e) {
      return e.getStatus();
    }
  }

  protected abstract IStatus doRun(final IProgressMonitor monitor) throws CoreException;
}
