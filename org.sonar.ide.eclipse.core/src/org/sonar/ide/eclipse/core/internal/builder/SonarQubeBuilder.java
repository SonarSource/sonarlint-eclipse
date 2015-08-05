/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
 * sonarqube@googlegroups.com
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

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.Map;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.jobs.AnalyzeProjectJob;
import org.sonar.ide.eclipse.core.internal.jobs.AnalyzeProjectRequest;

public class SonarQubeBuilder extends IncrementalProjectBuilder {

  @Override
  protected IProject[] build(int kind, Map args, IProgressMonitor monitor) {
    if (kind == IncrementalProjectBuilder.FULL_BUILD) {
      fullBuild(monitor);
    } else {
      IResourceDelta delta = getDelta(getProject());
      if (delta == null) {
        fullBuild(monitor);
      } else {
        incrementalBuild(delta, monitor);
      }
    }
    return null;
  }

  private void incrementalBuild(IResourceDelta delta, final IProgressMonitor monitor) {
    final Multimap<IProject, IFile> filesPerProject = LinkedHashMultimap.create();
    try {
      delta.accept(new IResourceDeltaVisitor() {
        @Override
        public boolean visit(IResourceDelta delta) {
          IResource resource = delta.getResource();
          if (!resource.exists() || resource.isDerived() || resource.isHidden()) {
            return false;
          }
          // Ignore changes on .project, .settings, ...
          if (resource.getName().startsWith(".")) {
            return false;
          }
          IFile file = (IFile) resource.getAdapter(IFile.class);
          if (file == null) {
            // visit children too
            return true;
          }
          IProject project = resource.getProject();
          filesPerProject.put(project, file);
          return true;
        }
      });
    } catch (CoreException e) {
      SonarCorePlugin.getDefault().error("Error during builder", e);
    }
    for (IProject project : filesPerProject.keys()) {
      AnalyzeProjectRequest request = new AnalyzeProjectRequest(project, filesPerProject.get(project), true);
      new AnalyzeProjectJob(request).schedule();

    }
  }

  private void fullBuild(IProgressMonitor monitor) {
    // Do nothing
  }
}
