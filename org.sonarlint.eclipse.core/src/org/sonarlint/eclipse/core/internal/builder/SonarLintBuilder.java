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
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
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
    final Map<IProject, Collection<IFile>> filesPerProject = new HashMap<>();
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
          IProject project = resource.getProject();
          if (!filesPerProject.containsKey(project)) {
            filesPerProject.put(project, new ArrayList<IFile>());
          }
          filesPerProject.get(project).add(file);
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

  public static boolean shouldAnalyze(IResourceDelta delta, IResource resource) {
    if (delta != null && delta.getKind() == IResourceDelta.REMOVED) {
      return false;
    }
    if (!resource.exists() || resource.isDerived(IResource.CHECK_ANCESTORS) || resource.isHidden(IResource.CHECK_ANCESTORS)) {
      return false;
    }
    // Ignore .project, .settings, ...
    if (resource.getName().startsWith(".")) {
      return false;
    }
    return true;
  }

  public static void addBuilder(IProject project) throws CoreException {
    IProjectDescription desc = project.getDescription();
    ICommand[] commands = desc.getBuildSpec();
    boolean found = false;

    for (int i = 0; i < commands.length; ++i) {
      if (commands[i].getBuilderName().equals(BUILDER_ID)) {
        found = true;
        break;
      }
    }
    if (!found) {
      // add builder to project
      ICommand command = desc.newCommand();
      command.setBuilderName(BUILDER_ID);
      ICommand[] newCommands = new ICommand[commands.length + 1];

      // Add it after other builders.
      System.arraycopy(commands, 0, newCommands, 0, commands.length);
      newCommands[commands.length] = command;
      desc.setBuildSpec(newCommands);
      project.setDescription(desc, null);
    }
  }

}
