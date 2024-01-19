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
package org.sonarlint.eclipse.core.internal.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;

public class JobUtils {

  /**
   * Wait for Future in a IRunnableWithProgress, throwing InterruptedException on cancellation, and InvocationTargetException on other exception, as specified
   * @see IRunnableWithProgress
   * @throws InterruptedException
   * @throws InvocationTargetException
   */
  public static <T> T waitForFutureInIRunnableWithProgress(IProgressMonitor monitor, CompletableFuture<T> future) throws InterruptedException, InvocationTargetException {
    try {
      return waitForFuture(monitor, future);
    } catch (CancellationException e) {
      var newEx = new InterruptedException("Operation cancelled");
      newEx.addSuppressed(e);
      throw newEx;
    } catch (ExecutionException e) {
      throw new InvocationTargetException(e.getCause() != null ? e.getCause() : e);
    }
  }

  public static <T> T waitForFuture(IProgressMonitor monitor, CompletableFuture<T> future) throws InterruptedException, ExecutionException {
    while (true) {
      if (monitor.isCanceled()) {
        future.cancel(true);
      }
      try {
        return future.get(100, TimeUnit.MILLISECONDS);
      } catch (TimeoutException t) {
        continue;
      }
    }
  }

  /**
   * Run something after the job is done, regardless of result.
   * Important: call job.schedule() after calling this method, NOT before.
   */
  public static void scheduleAfter(Job job, Runnable runnable) {
    job.addJobChangeListener(new JobCompletionListener() {
      @Override
      public void done(IJobChangeEvent event) {
        runnable.run();
      }
    });
  }

  /**
   * Run something after the job is done, with success. Do nothing if failed.
   * Important: call job.schedule() after calling this method, NOT before.
   */
  public static void scheduleAfterSuccess(Job job, Runnable runnable) {
    job.addJobChangeListener(new JobCompletionListener() {
      @Override
      public void done(IJobChangeEvent event) {
        if (event.getResult().isOK()) {
          runnable.run();
        }
      }
    });
  }

  abstract static class JobCompletionListener implements IJobChangeListener {
    @Override
    public void aboutToRun(IJobChangeEvent event) {
      // nothing to do
    }

    @Override
    public void awake(IJobChangeEvent event) {
      // nothing to do
    }

    @Override
    public void running(IJobChangeEvent event) {
      // nothing to do
    }

    @Override
    public void scheduled(IJobChangeEvent event) {
      // nothing to do
    }

    @Override
    public void sleeping(IJobChangeEvent event) {
      // nothing to do
    }
  }

  private JobUtils() {
    // utility class
  }
}
