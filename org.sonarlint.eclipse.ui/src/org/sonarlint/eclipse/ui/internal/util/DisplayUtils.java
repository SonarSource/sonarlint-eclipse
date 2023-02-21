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
package org.sonarlint.eclipse.ui.internal.util;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

public class DisplayUtils {

  @Nullable
  public static <T> T syncExec(Supplier<T> supplier) {
    var runnable = new RunnableWithResult<>(supplier);
    Display.getDefault().syncExec(runnable);
    return runnable.getResult();
  }

  public static <T> CompletableFuture<T> asyncExec(Supplier<T> supplier) {
    var runnable = new FutureSupplierResult<>(supplier);
    Display.getDefault().asyncExec(runnable);
    return runnable.getFuture();
  }

  public static CompletableFuture<Void> asyncExec(Runnable runnable) {
    var r = new FutureRunnableResult(runnable);
    Display.getDefault().asyncExec(r);
    return r.getFuture();
  }

  public static CompletableFuture<Void> bringToFrontAsync() {
    return asyncExec(DisplayUtils::bringToFront);
  }

  public static void bringToFront() {
    var window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    var shell = window.getShell();
    if (shell != null) {
      if (shell.getMinimized()) {
        shell.setMinimized(false);
      }
      shell.forceActive();
    }
  }

  private static class RunnableWithResult<T> implements Runnable {

    private final Supplier<T> supplier;
    @Nullable
    private T result;

    private RunnableWithResult(Supplier<T> supplier) {
      this.supplier = supplier;
    }

    public T getResult() {
      return result;
    }

    @Override
    public void run() {
      result = supplier.get();
    }
  }

  private static class FutureSupplierResult<T> implements Runnable {

    private final CompletableFuture<T> future;
    private final Supplier<T> supplier;

    private FutureSupplierResult(Supplier<T> supplier) {
      future = new CompletableFuture<>();
      this.supplier = supplier;
    }

    public CompletableFuture<T> getFuture() {
      return future;
    }

    @Override
    public void run() {
      var result = supplier.get();
      if (result != null) {
        future.complete(result);
      } else {
        future.cancel(false);
      }
    }
  }

  private static class FutureRunnableResult implements Runnable {

    private final CompletableFuture<Void> future;
    private final Runnable runnable;

    private FutureRunnableResult(Runnable runnable) {
      future = new CompletableFuture<>();
      this.runnable = runnable;
    }

    public CompletableFuture<Void> getFuture() {
      return future;
    }

    @Override
    public void run() {
      runnable.run();
      future.complete(null);
    }
  }

  private DisplayUtils() {
    // utility class
  }

}
