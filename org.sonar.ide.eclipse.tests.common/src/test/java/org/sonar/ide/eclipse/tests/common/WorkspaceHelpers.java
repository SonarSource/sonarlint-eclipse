/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2013 SonarSource
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
package org.sonar.ide.eclipse.tests.common;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import java.io.File;
import java.io.IOException;

/**
 * @author Evgeny Mandrikov
 */
public final class WorkspaceHelpers {

  public static void cleanWorkspace() throws CoreException, IOException {
    doCleanWorkspace();
  }

  private static void doCleanWorkspace() throws CoreException, IOException {
    final IWorkspace workspace = ResourcesPlugin.getWorkspace();
    workspace.run(new IWorkspaceRunnable() {

      public void run(IProgressMonitor monitor) throws CoreException {
        IProject[] projects = workspace.getRoot().getProjects();
        for (IProject project : projects) {
          project.delete(true, true, monitor);
        }
      }
    }, new NullProgressMonitor());

    JobHelpers.waitForJobsToComplete();

    File[] files = workspace.getRoot().getLocation().toFile().listFiles();
    if (files != null) {
      for (File file : files) {
        if (!".metadata".equals(file.getName())) {
          if (file.isDirectory()) {
            FileUtils.deleteDirectory(file);
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
