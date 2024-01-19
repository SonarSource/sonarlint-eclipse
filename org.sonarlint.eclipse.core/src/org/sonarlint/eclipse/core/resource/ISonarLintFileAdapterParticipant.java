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
package org.sonarlint.eclipse.core.resource;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.annotation.Nullable;

/**
 * By default SonarLint try to adapt any {@link IFile} to {@link ISonarLintFile}.
 * You can use this extension point to prevent some {@link IFile} to be converted to {@link ISonarLintFile}
 * or to provide your own implementation.
 * @since 3.0
 */
public interface ISonarLintFileAdapterParticipant {

  /**
   * @return <code>true</code> to have this file not converted to an {@link ISonarLintFile}
   */
  default boolean exclude(IFile file) {
    return false;
  }

  /**
   * @return <code>null</code> to use SonarLint default adapter
   */
  @Nullable
  default ISonarLintFile adapt(IFile file) {
    return null;
  }

}
