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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectJob;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProject;

import static org.sonarlint.eclipse.core.internal.utils.SonarLintUtils.removeAggregatedDuplicates;

public class SonarLintChangeListener implements IResourceChangeListener {

  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
      final Map<IProject, Collection<IFile>> changedFilesPerProject = new HashMap<>();
      try {
        event.getDelta().accept(delta -> visitDelta(changedFilesPerProject, delta));
      } catch (CoreException e) {
        SonarLintCorePlugin.getDefault().error(e.getMessage(), e);
      }

      removeAggregatedDuplicates(changedFilesPerProject);

      for (Map.Entry<IProject, Collection<IFile>> entry : changedFilesPerProject.entrySet()) {
        IProject project = entry.getKey();
        Collection<IFile> filesToAnalyze = entry.getValue();
        if (filesToAnalyze.size() > 10) {
          SonarLintCorePlugin.getDefault().debug("Too many files to analyze in project " + project.getName() + " (" + filesToAnalyze.size() + "). Skipping.");
          return;
        }
        AnalyzeProjectRequest request = new AnalyzeProjectRequest(project, filesToAnalyze, TriggerType.EDITOR_CHANGE);
        new AnalyzeProjectJob(request).schedule();
      }
    }
  }

  private static boolean visitDelta(final Map<IProject, Collection<IFile>> changedFilesPerProject, IResourceDelta delta) {
    IProject project = delta.getResource().getProject();
    if (project == null) {
      // Workspace root
      return true;
    }
    if (!project.isAccessible()) {
      return false;
    }
    final SonarLintProject sonarProject = SonarLintProject.getInstance(project);
    if (!sonarProject.isAutoEnabled()) {
      return false;
    }
    if (delta.getResource().getType() == IResource.FILE) {
      IFile file = (IFile) delta.getResource();
      if (delta.getKind() == IResourceDelta.CHANGED
        && (delta.getFlags() & IResourceDelta.CONTENT) != 0
        && shouldAnalyze(file)) {
        if (!changedFilesPerProject.containsKey(file.getProject())) {
          changedFilesPerProject.put(file.getProject(), new ArrayList<IFile>());
        }
        changedFilesPerProject.get(file.getProject()).add(file);
      }
    }
    return shouldAnalyze(delta.getResource());
  }

  public static boolean shouldAnalyze(IResource resource) {
    if (!resource.exists() || resource.isDerived(IResource.CHECK_ANCESTORS) || resource.isHidden(IResource.CHECK_ANCESTORS)) {
      return false;
    }
    // Ignore .project, .settings, that are not considered hidden on Windows...
    // Also ignore .class (SLE-65)
    if (resource.getName().startsWith(".") || "class".equals(resource.getFileExtension())) {
      return false;
    }
    return true;
  }
}
