/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2019 SonarSource SA
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
package org.sonarlint.eclipse.core.analysis;

import org.sonarlint.eclipse.core.resource.ISonarLintFile;

/**
 * By default SonarLint decides what sources are tests based on a user configurable file path pattern.
 * You can use this extension point to influence that.
 * @since 3.1
 */
public interface IFileTypeProvider {

  public enum ISonarLintFileType {
    UNKNOWN,
    MAIN,
    TEST
  }

  /**
   * @return {@link IFileTypeProvider.ISonarLintFileType#UNKNOWN} to use SonarLint default strategy
   */
  ISonarLintFileType qualify(ISonarLintFile file);

}
