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
package org.sonarlint.eclipse.core.internal.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.core.runtime.IProgressMonitor;

public class JobUtils {

  public static <T> T waitForFuture(IProgressMonitor monitor, CompletableFuture<T> future) throws InterruptedException, InvocationTargetException {
    while (true) {
      if (monitor.isCanceled()) {
        future.cancel(true);
      }
      try {
        return future.get(100, TimeUnit.MILLISECONDS);
      } catch (TimeoutException t) {
        continue;
      } catch (InterruptedException e) {
        throw new InterruptedException("Interrupted");
      } catch (CancellationException e) {
        throw new InterruptedException("Operation cancelled");
      } catch (ExecutionException e) {
        throw new InvocationTargetException(e.getCause() != null ? e.getCause() : e);
      }
    }
  }

  private JobUtils() {
    // utility class
  }
}
