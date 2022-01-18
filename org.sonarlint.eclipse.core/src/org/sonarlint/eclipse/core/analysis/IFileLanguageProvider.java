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
package org.sonarlint.eclipse.core.analysis;

import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;

/**
 * Most analyzers are relying on file extensions to detect languages. In some situations it
 * may be useful to force language.
 * @since 3.0
 */
public interface IFileLanguageProvider {

  /**
   * @return the language of the file, or null to keep default bahavior
   */
  @Nullable
  String language(ISonarLintFile file);

}
