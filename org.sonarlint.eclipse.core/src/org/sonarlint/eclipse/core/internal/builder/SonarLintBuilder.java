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
package org.sonarlint.eclipse.core.internal.builder;

import java.util.ArrayList;
import java.util.Collection;
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
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectJob;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest;

public class SonarLintBuilder extends IncrementalProjectBuilder {

  public static final String BUILDER_ID = SonarLintCorePlugin.PLUGIN_ID + ".sonarlintBuilder";

  @Override
  protected IProject[] build(int kind, Map args, IProgressMonitor monitor) {
    if (kind != IncrementalProjectBuilder.FULL_BUILD) {
      IResourceDelta delta = getDelta(getProject());
      if (delta != null) {
        incrementalBuild(delta);
      }
    }
    return null;
  }

  private void incrementalBuild(IResourceDelta delta) {
    final Collection<IFile> filesToAnalyze = new ArrayList<>();
    try {
      delta.accept(new IResourceDeltaVisitor() {
        @Override
        public boolean visit(IResourceDelta delta) {
          IResource resource = delta.getResource();
          if (!shouldAnalyze(delta, resource)) {
            return false;
          }
          IFile file = (IFile) resource.getAdapter(IFile.class);
          if (file == null) {
            // visit children too
            return true;
          }
          filesToAnalyze.add(file);
          return true;
        }
      });
    } catch (CoreException e) {
      SonarLintCorePlugin.getDefault().error("Error during builder", e);
    }
    if (filesToAnalyze.size() > 10) {
      SonarLintCorePlugin.getDefault().debug("Too many files to analyze in project " + getProject().getName() + " (" + filesToAnalyze.size() + "). Skipping.");
    } else if (!filesToAnalyze.isEmpty()) {
      AnalyzeProjectRequest request = new AnalyzeProjectRequest(getProject(), filesToAnalyze);
      new AnalyzeProjectJob(request).schedule();
    }
  }

  public static boolean shouldAnalyze(IResourceDelta delta, IResource resource) {
    if (delta != null && delta.getKind() == IResourceDelta.REMOVED) {
      return false;
    }
    return shouldAnalyze(resource);
  }

  public static boolean shouldAnalyze(IResource resource) {
    if (!resource.exists() || resource.isDerived(IResource.CHECK_ANCESTORS) || resource.isHidden(IResource.CHECK_ANCESTORS)) {
      return false;
    }
    // Ignore .project, .settings, ...
    if (resource.getName().startsWith(".")) {
      return false;
    }
    return true;
  }

}
