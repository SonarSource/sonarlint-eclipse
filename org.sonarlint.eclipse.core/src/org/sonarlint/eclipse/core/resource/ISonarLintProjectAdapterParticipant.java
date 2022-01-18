/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2022 SonarSource SA
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

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.annotation.Nullable;

/**
 * By default SonarLint try to adapt any {@link IProject} to {@link ISonarLintProject}.
 * You can use this extension point to prevent some {@link IProject} to be converted to {@link ISonarLintProject}
 * or to provide your own implementation.
 * @since 3.0
 */
public interface ISonarLintProjectAdapterParticipant {

  /**
   * @return <code>true</code> to have this project not converted to an {@link ISonarLintProject}
   */
  default boolean exclude(IProject project) {
    return false;
  }

  /**
   * Adapt an {@link IProject} to an {@link ISonarLintProject}.
   * @return <code>null</code> if this is not a project you manage
   * @deprecated since 4.1 implements {@link #adapt(IProject, ISonarLintProject)} instead
   */
  @Deprecated
  @Nullable
  default ISonarLintProject adapt(IProject project) {
    return null;
  }

  /**
   * Adapt an {@link IProject} to an {@link ISonarLintProject}.
   * @param defaultAdapter this is the default adapter of SonarLint. You can use it as a delegate if
   * you want to only override some parts of the behavior.
   * @return <code>null</code> if this is not a project you manage
   * @since 4.1
   */
  @Nullable
  default ISonarLintProject adapt(IProject project, ISonarLintProject defaultAdapter) {
    return adapt(project);
  }
}
