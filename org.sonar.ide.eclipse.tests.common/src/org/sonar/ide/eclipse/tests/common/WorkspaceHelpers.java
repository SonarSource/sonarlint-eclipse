/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.tests.common;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * @author Evgeny Mandrikov
 */
public class WorkspaceHelpers {

  public static void cleanWorkspace() throws InterruptedException, CoreException, IOException {
    // TODO Godin: implement me
    doCleanWorkspace();
  }

  private static void doCleanWorkspace() throws InterruptedException, CoreException, IOException {
    final IWorkspace workspace = ResourcesPlugin.getWorkspace();
    workspace.run(new IWorkspaceRunnable() {

      public void run(IProgressMonitor monitor) throws CoreException {
        IProject[] projects = workspace.getRoot().getProjects();
        for (int i = 0; i < projects.length; i++) {
          projects[i].delete(true, true, monitor);
        }
      }
    }, new NullProgressMonitor());

    JobHelpers.waitForJobsToComplete(); // was: JobHelpers.waitForJobsToComplete(new NullProgressMonitor());

    File[] files = workspace.getRoot().getLocation().toFile().listFiles();
    if (files != null) {
      for (File file : files) {
        if ( !".metadata".equals(file.getName())) {
          if (file.isDirectory()) {
            FileUtils.deleteDirectory(file);
          } else {
            if ( !file.delete()) {
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
