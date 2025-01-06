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
package org.sonarlint.eclipse.ui.internal.job;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarsource.sonarlint.core.rpc.protocol.client.progress.ProgressUpdateNotification;
import org.sonarsource.sonarlint.core.rpc.protocol.client.progress.StartProgressParams;

/**
 *  As the backend is now in charge of synchronization we want the user to see the progress even though it was not
 *  scheduled on the IDE side. Therefore we create fake jobs for every backend job which offers interaction
 *  possibilities for the user.
 */
public class BackendProgressJobScheduler {
  private static final BackendProgressJobScheduler INSTANCE = new BackendProgressJobScheduler();
  private final ConcurrentHashMap<String, BackendProgressJob> jobPool = new ConcurrentHashMap<>();

  private BackendProgressJobScheduler() {
  }

  public static BackendProgressJobScheduler get() {
    return INSTANCE;
  }

  /** Start a new progress bar by using an IDE job */
  public void startProgress(StartProgressParams params) throws UnsupportedOperationException {
    var taskId = params.getTaskId();
    if (jobPool.containsKey(taskId)) {
      var errorMessage = "Job with ID " + taskId + " is already active, skip reporting it";
      SonarLintLogger.get().debug(errorMessage);
      throw new CancellationException(errorMessage);
    }

    jobPool.computeIfAbsent(taskId, k -> {
      var job = new BackendProgressJob(params);
      job.schedule();
      return job;
    });
  }

  /** Update the progress bar IDE job */
  public void update(String taskId, ProgressUpdateNotification notification) {
    var job = jobPool.get(taskId);
    if (job == null) {
      SonarLintLogger.get().debug("Job with ID " + taskId + " is unknown, skip reporting it");
      return;
    }
    job.update(notification);
  }

  /** Complete the progress bar IDE job */
  public void complete(String taskId) {
    var job = jobPool.remove(taskId);
    if (job == null) {
      SonarLintLogger.get().debug("Job with ID " + taskId + " is unknown, skip reporting it");
      return;
    }
    job.complete();
  }

  /** This job is only an IDE frontend for a job running in the SonarLintBackend */
  private static class BackendProgressJob extends Job {
    private final Object waitMonitor = new Object();
    private final AtomicReference<String> message;
    private final AtomicInteger percentage = new AtomicInteger(0);
    private final AtomicBoolean complete = new AtomicBoolean(false);

    public BackendProgressJob(StartProgressParams params) {
      super(params.getTitle());
      setPriority(DECORATE);

      this.message = new AtomicReference<>(params.getMessage());
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      monitor.setTaskName(message.get());
      monitor.worked(percentage.get());

      while (!complete.get()) {
        synchronized (waitMonitor) {
          monitor.setTaskName(message.get());
          monitor.worked(percentage.get());
        }
      }

      monitor.done();
      return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
    }

    public void update(ProgressUpdateNotification notification) {
      var newMessage = notification.getMessage();
      if (newMessage != null) {
        message.set(newMessage);
      }
      percentage.set(notification.getPercentage());

      synchronized (waitMonitor) {
        waitMonitor.notify();
      }
    }

    public void complete() {
      complete.set(true);
      synchronized (waitMonitor) {
        waitMonitor.notify();
      }
    }
  }
}
