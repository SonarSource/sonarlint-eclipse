/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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
package org.sonarlint.eclipse.core.internal;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProject;

public class NewProjectListener implements IResourceChangeListener {

  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
      List<IProject> projects = getProjects(event.getDelta());
      for (final IProject p : projects) {
        WorkspaceJob job = new WorkspaceJob("Enable SonarLint builder") {

          @Override
          public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
            SonarLintProject.getInstance(p).setBuilderEnabled(true, monitor);
            return Status.OK_STATUS;
          }
        };
        job.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
        job.schedule();
      }
    }
  }

  private static List<IProject> getProjects(IResourceDelta delta) {
    final List<IProject> projects = new ArrayList<>();
    try {
      delta.accept(new IResourceDeltaVisitor() {
        @Override
        public boolean visit(IResourceDelta delta) throws CoreException {
          if (delta.getKind() == IResourceDelta.ADDED &&
            delta.getResource().getType() == IResource.PROJECT) {
            IProject project = (IProject) delta.getResource();
            if (project.isAccessible()) {
              projects.add(project);
            }
          }
          // only continue for the workspace root
          return delta.getResource().getType() == IResource.ROOT;
        }
      });
    } catch (CoreException e) {
      throw new IllegalStateException(e);
    }
    return projects;
  }
}
