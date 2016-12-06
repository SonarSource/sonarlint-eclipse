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
package org.sonarlint.eclipse.core.internal.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.junit.Test;
import org.sonarlint.eclipse.tests.common.JobHelpers;
import org.sonarlint.eclipse.tests.common.SonarTestCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarlint.eclipse.core.internal.utils.SonarLintUtils.aggregatePerMoreSpecificProject;

public class SonarLintUtilsTest extends SonarTestCase {

  @Test
  public void use_more_precise_project() throws Exception {
    IProject root = importEclipseProject("multimodule");
    IProject module = workspace.getRoot().getProject("submodule");
    workspace.run(new IWorkspaceRunnable() {

      @Override
      public void run(final IProgressMonitor monitor) throws CoreException {
        final IProjectDescription projectDescription = workspace.newProjectDescription(module.getName());
        projectDescription.setLocation(root.findMember("submodule").getRawLocation());
        module.create(projectDescription, monitor);
        module.open(IResource.NONE, monitor);
      }
    }, workspace.getRoot(), IWorkspace.AVOID_UPDATE, MONITOR);
    JobHelpers.waitForJobsToComplete();

    Map<IProject, Collection<IFile>> usePrecise = aggregatePerMoreSpecificProject(Arrays.asList(root.getFile("submodule/foo.js")));
    assertThat(usePrecise).containsOnlyKeys(module);
    assertThat(usePrecise.get(module)).containsOnly(module.getFile("foo.js"));

    Map<IProject, Collection<IFile>> aggregate = aggregatePerMoreSpecificProject(Arrays.asList(root.getFile("submodule/foo.js"), module.getFile("foo.js")));
    assertThat(aggregate).containsOnlyKeys(module);
    assertThat(aggregate.get(module)).containsOnly(module.getFile("foo.js"));
  }

}
