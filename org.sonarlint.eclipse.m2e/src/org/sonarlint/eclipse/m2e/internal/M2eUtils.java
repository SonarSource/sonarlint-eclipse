/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2020 SonarSource SA
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
package org.sonarlint.eclipse.m2e.internal;

import org.eclipse.core.internal.localstore.FileSystemResourceManager;
import org.eclipse.core.internal.resources.ICoreConstants;
import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.sonarlint.eclipse.core.SonarLintLogger;

public class M2eUtils {

  private M2eUtils() {
  }

  /**
   * m2e creates IProject for every module (flat layout). It means the same file can appear in several projects.
   * When it is the case we want to keep the most specific one.
   */
  public static boolean isInNestedModule(IFile file) {
    IProject project = file.getProject();
    try {
      if (project.hasNature(IMavenConstants.NATURE_ID)) {
        IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();

        IMavenProjectFacade projectFacade = projectManager.create(project, null);
        if (projectFacade != null && "pom".equals(projectFacade.getPackaging())) {
          return !toSpecificFile(file).equals(file);
        }
      }
    } catch (CoreException ex) {
      SonarLintLogger.get().error(ex.getMessage(), ex);
    }
    return false;
  }

  private static IFile toSpecificFile(IFile file) {
    IFile finalFile = file;
    IPath rawLocation = file.getRawLocation();
    if (rawLocation != null) {
      // TODO use getFileForLocation when we will support only Eclipse 4.6+
      // IFile moreSpecific = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(rawLocation);
      IFile moreSpecific = resourceForLocation(rawLocation);
      if (moreSpecific != null) {
        finalFile = moreSpecific;
      }
    }
    return finalFile;
  }

  /**
   * Copied from {@link FileSystemResourceManager} of Oxygen to support older Eclipse versions
   */
  private static IFile resourceForLocation(IPath location) {
    int resultProjectPathSegments = 0;
    IFile result = null;
    IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects(IContainer.INCLUDE_HIDDEN);
    for (int i = 0; i < projects.length; i++) {
      IProject project = projects[i];
      IPath projectLocation = project.getLocation();
      if (projectLocation != null && projectLocation.isPrefixOf(location)) {
        int segmentsToRemove = projectLocation.segmentCount();
        if (segmentsToRemove > resultProjectPathSegments) {
          IPath path = project.getFullPath().append(location.removeFirstSegments(segmentsToRemove));
          IFile resource = resourceFor(path);
          if (resource != null && !((Resource) resource).isFiltered()) {
            resultProjectPathSegments = segmentsToRemove;
            result = resource;
          }
        }
      }
    }
    return result;
  }

  private static IFile resourceFor(IPath path) {
    int numSegments = path.segmentCount();
    if (numSegments < ICoreConstants.MINIMUM_FILE_SEGMENT_LENGTH) {
      return null;
    }
    return ResourcesPlugin.getWorkspace().getRoot().getFile(path);
  }

}
