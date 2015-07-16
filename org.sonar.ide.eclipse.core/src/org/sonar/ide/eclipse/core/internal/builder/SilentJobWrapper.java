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
package org.sonar.ide.eclipse.core.internal.builder;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.jobs.functions.IJobFunction;

/**
 * This is a {@link Job} worker and it is executed  in background silently.
 * This {@link Job} wraps job functions {@link IJobFunction}, any errors during execution of the functions are silently logged to
 * debug level without use facing warning or error pop-ups.<br>
 *
 * <p>This job wrapper logs {@link Status} with severities {@link IStatus#ERROR} & {@link IStatus#INFO} for the given functions and
 *  {@link Exception} thrown by the given functions if any. The errors and exception are logged to debug level.</p>
 *
 * @author Hemantkumar Chigadani
 */
@SuppressWarnings("nls")
final class SilentJobWrapper extends Job {

  private static final int LIMIT = 2;

  private static final Object JOB_FAMILY = SilentJobWrapper.class.getName();

  private final IJobFunction[] iJobFunctions;

  private boolean rescheduled;

  /**
   * @param functions , can not be <code>null</code> but it's element can be null
   */
  SilentJobWrapper(final IJobFunction... functions) {
    super("Building Sonar projects");
    this.iJobFunctions = functions;
    setPriority(Job.BUILD);
    // Limit no of workers can be active at a given time to avoid any fall-over of heavy fetching & analysing task.
    addJobChangeListener(new JobChangeAdapter() {
      /**
       * {@inheritDoc}
       */
      @Override
      public void aboutToRun(IJobChangeEvent event) {
        super.aboutToRun(event);
        if (isConflicting()) {
          SilentJobWrapper.this.rescheduled = true;
          SilentJobWrapper.this.cancel();
          SilentJobWrapper.this.schedule(3000);
        } else {
          SilentJobWrapper.this.rescheduled = false;
          SilentJobWrapper.this.removeJobChangeListener(this);
        }

      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected IStatus run(final IProgressMonitor monitor) {
    IStatus iStatus = Status.OK_STATUS;
    for (final IJobFunction iJobFunction : iJobFunctions) {
      if (iJobFunction != null) {
        iStatus = runFunct(monitor, iJobFunction);
        if (iStatus != Status.OK_STATUS) {
          // Continue only if previous ones are OK.
          break;
        }
      }
    }
    return iStatus;
  }

  /**
   * @param monitor
   * @param iJobFunction
   * @return
   */
  private IStatus runFunct(final IProgressMonitor monitor, final IJobFunction iJobFunction) {
    IStatus iStatus = null;
    try {

      iStatus = iJobFunction.run(monitor);

      // Log error/exceptions to debug level.
    } catch (final Exception exception) {

      SonarCorePlugin.getDefault().debug(exception.getMessage());
    }
    if (iStatus != null && (iStatus.getSeverity() == IStatus.ERROR || iStatus.getSeverity() == IStatus.INFO)) {

      SonarCorePlugin.getDefault().debug(iStatus.getMessage());
      iStatus = Status.OK_STATUS;
    }
    return iStatus;
  }

  /**
   * @return the rescheduled
   */
  final boolean isRescheduled() {
    return rescheduled;
  }

  private boolean isConflicting() {
    boolean isConflicting;
    final IJobManager jobManager = getJobManager();
    final Job[] find = jobManager.find(JOB_FAMILY);

    int activeJobsize = 0;
    for (Job job : find) {
      if (job.getState() == Job.RUNNING) {
        ++activeJobsize;
      }
    }
    isConflicting = false;
    if (activeJobsize >= LIMIT) {
      isConflicting = true;

    }
    return isConflicting;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean belongsTo(final Object family) {
    return family.equals(JOB_FAMILY) ? true : super.belongsTo(family);
  }

}
