/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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
package org.sonarlint.eclipse.core.resource;

import java.util.Collection;
import java.util.Collections;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 *  As Eclipse by default treats everything like a stand-alone project, hierarchical structures like Maven / Gradle
 *  with their modules / sub-projects are not easily taken into account. In some cases, having the information that an
 *  @{link IProject} is part of a hierarchy as well as getting the root @{link IProject} might be beneficial.
 *  @since 10.1
 */
public interface ISonarLintProjectHierarchyProvider {
  /**
   *  This is used to identify the specific provider in case multiple are available. This has to be implemented, even
   *  though a conflict in identifiers of different implementations might be possible!
   *
   *  @return the "unique" identifier of the specific provider
   */
  @NonNull
  String getHierarchyProviderIdentifier();

  /**
   *  This is used to check if a {@link ISonarLintProject} is part of a hierarchy. As an example, a Maven project with
   *  neither a parent project nor sub-modules is considered to be in a hierarchy.
   *
   *  @param project to check
   *  @return true if it is, false otherwise
   */
  default boolean partOfHierarchy(ISonarLintProject project) {
    return false;
  }

  /**
   *  This tries to get the root project of the hierarchy itself inside the workspace. As an example, a Maven parent
   *  project with sub-modules will return itself as it is the root project of the hierarchy.
   *  Contract applies that {@link ISonarLintProjectHierarchyProvider#partOfHierarchy(ISonarLintProject)} was called
   *  before and returned true.
   *
   *  @param project to get root project from
   *  @return root project if found in workspace, null otherwise
   */
  @Nullable
  default ISonarLintProject getRootProject(ISonarLintProject project) {
    return null;
  }

  /**
   *  This tries to get the sub-projects of the given project itself inside the workspace. As an example, a Maven
   *  project without sub-modules will return an empty collection
   *  Contract applies that {@link ISonarLintProjectHierarchyProvider#partOfHierarchy(ISonarLintProject)} was called
   *  before and returned true.
   *
   *  @param project to get sub-projects from
   *  @return sub-projects if found in workspace, null otherwise
   */
  default Collection<ISonarLintProject> getSubProjects(ISonarLintProject project) {
    return Collections.emptyList();
  }
}
