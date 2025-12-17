/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource Sàrl
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
package org.sonarlint.eclipse.ui.internal.binding;

import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.config.binding.BindingSuggestionOrigin;

/** Pairing of the information of a project and the suggestion origin */
public class ProjectSuggestionDto {
  private final ISonarLintProject project;
  private final BindingSuggestionOrigin origin;

  public ProjectSuggestionDto(ISonarLintProject project, BindingSuggestionOrigin origin) {
    this.project = project;
    this.origin = origin;
  }

  public ISonarLintProject getProject() {
    return this.project;
  }

  public BindingSuggestionOrigin getOrigin() {
    return origin;
  }
}
