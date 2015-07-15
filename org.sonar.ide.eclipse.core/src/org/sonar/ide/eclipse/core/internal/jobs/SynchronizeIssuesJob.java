/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
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
package org.sonar.ide.eclipse.core.internal.jobs;

import java.util.List;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.sonar.ide.eclipse.core.internal.jobs.functions.SynchronizeIssuesJobFunction;

/**
 * This class load issues in background.
 *
 */
@SuppressWarnings("nls")
public class SynchronizeIssuesJob extends Job {

  public static final Object REMOTE_SONAR_JOB_FAMILY = new Object();

  private IProgressMonitor monitor;

  private final SynchronizeIssuesJobFunction synchronizeIssuesJobFunction;

  public SynchronizeIssuesJob(final List<? extends IResource> resources, final boolean force) {
    super("Synchronize issues");
    setPriority(Job.LONG);
    synchronizeIssuesJobFunction = new SynchronizeIssuesJobFunction(resources, force);

  }

  @Override
  protected IStatus run(final IProgressMonitor monitor) {
    this.monitor = monitor;
    return synchronizeIssuesJobFunction.run(monitor);
  }

  public IProgressMonitor getMonitor() {
    return monitor;
  }

  @Override
  public boolean belongsTo(final Object family) {
    return family.equals(REMOTE_SONAR_JOB_FAMILY) ? true : super.belongsTo(family);
  }

}
