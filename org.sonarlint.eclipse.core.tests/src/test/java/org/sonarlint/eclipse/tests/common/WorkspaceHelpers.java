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
package org.sonarlint.eclipse.tests.common;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;

/**
 * Inspired by m2e
 */
public final class WorkspaceHelpers {

  public static void cleanWorkspace() throws InterruptedException, CoreException {
    Exception cause = null;
    int i;
    for (i = 0; i < 10; i++) {
      try {
        System.gc();
        doCleanWorkspace();
      } catch (InterruptedException e) {
        throw e;
      } catch (OperationCanceledException e) {
        throw e;
      } catch (Exception e) {
        cause = e;
        e.printStackTrace();
        System.out.println(i);
        Thread.sleep(6 * 1000);
        continue;
      }

      // all clear
      return;
    }

    // must be a timeout
    throw new CoreException(new Status(IStatus.ERROR, SonarLintCorePlugin.PLUGIN_ID,
      "Could not delete workspace resources (after " + i + " retries): "
        + List.of(ResourcesPlugin.getWorkspace().getRoot().getProjects()),
      cause));
  }

  private static void doCleanWorkspace() throws InterruptedException, CoreException, IOException {
    final var workspace = ResourcesPlugin.getWorkspace();
    workspace.run(new IWorkspaceRunnable() {
      @Override
      public void run(IProgressMonitor monitor) throws CoreException {
        var projects = workspace.getRoot().getProjects();
        for (var project : projects) {
          project.delete(true, true, monitor);
        }
      }
    }, new NullProgressMonitor());

    JobHelpers.waitForJobsToComplete(new NullProgressMonitor());

    var files = workspace.getRoot().getLocation().toFile().listFiles();
    if (files != null) {
      for (var file : files) {
        if (!".metadata".equals(file.getName())) {
          if (file.isDirectory()) {
            Files.walkFileTree(file.toPath(), new SimpleFileVisitor<Path>() {
              @Override
              public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
              }

              @Override
              public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
              }

            });
          } else {
            if (!file.delete()) {
              throw new IOException("Could not delete file " + file.getCanonicalPath());
            }
          }
        }
      }
    }
  }

  private WorkspaceHelpers() {
  }
}
