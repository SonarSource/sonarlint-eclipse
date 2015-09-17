/*
 * SonarLint for Eclipse
 * Copyright (C) 2015 SonarSource
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
package org.sonarlint.eclipse.core.internal.builder;

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
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.SonarLintNature;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectJob;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest;

public class SonarLintBuilder extends IncrementalProjectBuilder {

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
    SonarLintCorePlugin.getDefault().debug("BUILDER: incremental build on " + getProject() + "\n");
    final Multimap<IProject, IFile> filesPerProject = LinkedHashMultimap.create();
    try {
      delta.accept(new IResourceDeltaVisitor() {
        @Override
        public boolean visit(IResourceDelta delta) {
          IResource resource = delta.getResource();
          if (!SonarLintNature.hasSonarLintNature(resource.getProject()) || !resource.exists() || resource.isDerived() || resource.isHidden()) {
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
          SonarLintCorePlugin.getDefault().debug("BUILDER: file changed " + file + " on project " + project + "\n");
          return true;
        }
      });
    } catch (CoreException e) {
      SonarLintCorePlugin.getDefault().error("Error during builder", e);
    }
    for (IProject project : filesPerProject.keySet()) {
      AnalyzeProjectRequest request = new AnalyzeProjectRequest(project, filesPerProject.get(project));
      new AnalyzeProjectJob(request).schedule();

    }
  }

  private void fullBuild(IProgressMonitor monitor) {
    SonarLintCorePlugin.getDefault().debug("BUILDER: full build on " + getProject() + "\n");
  }
}
