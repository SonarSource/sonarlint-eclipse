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

import java.util.Collections;
import java.util.Map;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.sonar.ide.eclipse.core.internal.jobs.AnalyzeProjectRequest;
import org.sonar.ide.eclipse.core.internal.jobs.SonarQubeAnalysisJob;

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

  private void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor) {
    System.out.println("incremental build on " + delta);
    try {
      delta.accept(new IResourceDeltaVisitor() {
        @Override
        public boolean visit(IResourceDelta delta) {
          IResource resource = delta.getResource();
          if (resource.isDerived()) {
            return false;
          }
          IFile file = (IFile) resource.getAdapter(IFile.class);
          if (file == null) {
            return true;
          }
          System.out.println("changed: " + file.getRawLocation());
          IProject project = resource.getProject();
          AnalyzeProjectRequest request = new AnalyzeProjectRequest(file)
            // // FIXME .setDebugEnabled(false)
            .useHttpWsCache(true);
          new SonarQubeAnalysisJob(Collections.singletonList(request)).schedule();
          return true; // visit children too
        }
      });
    } catch (CoreException e) {
      e.printStackTrace();
    }
  }

  private void fullBuild(IProgressMonitor monitor) {
    System.out.println("full build");
  }
}
