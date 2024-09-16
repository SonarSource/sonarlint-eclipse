/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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
package org.sonarlint.eclipse.buildship.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.resource.IProjectScopeProvider;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.core.resource.ISonarLintProjectHierarchyProvider;

/**
 *  Just like Maven integration into Eclipse (m2e) there is a hierarchical project view inside the IDE.
 */
public class GradleProjectConfigurationExtension implements ISonarLintProjectHierarchyProvider, IProjectScopeProvider {
  private final boolean isToolingApiPresent;

  public GradleProjectConfigurationExtension() {
    this.isToolingApiPresent = isToolingApiPresent();
  }

  private static boolean isToolingApiPresent() {
    try {
      Class.forName("org.gradle.tooling.GradleConnector");
      Class.forName("org.gradle.tooling.ProjectConnection");
      Class.forName("org.gradle.tooling.model.gradle.BasicGradleProject");
      Class.forName("org.gradle.tooling.model.gradle.GradleBuild");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  @Override
  public String getHierarchyProviderIdentifier() {
    return "Gradle (found by the SonarLint Buildship adapter)";
  }

  @Override
  public boolean partOfHierarchy(ISonarLintProject project) {
    return isToolingApiPresent
      && GradleUtils.checkIfGradleProject((IProject) project.getResource());
  }

  @Override
  @Nullable
  public ISonarLintProject getRootProject(ISonarLintProject project) {
    if (isToolingApiPresent) {
      return GradleUtils.getRootProjectInWorkspace(project);
    }
    return null;
  }

  @Override
  public Collection<ISonarLintProject> getSubProjects(ISonarLintProject project) {
    if (isToolingApiPresent) {
      return GradleUtils.getProjectSubProjects(project);
    }
    return Collections.emptyList();
  }

  @Override
  public Set<IPath> getExclusions(IProject project) {
    if (isToolingApiPresent && GradleUtils.checkIfGradleProject(project)) {
      return GradleUtils.getExclusions(project);
    }
    return Collections.emptySet();
  }
}
