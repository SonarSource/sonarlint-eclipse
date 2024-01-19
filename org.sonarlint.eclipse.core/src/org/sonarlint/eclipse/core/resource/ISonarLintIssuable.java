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

import org.eclipse.core.resources.IResource;

/**
 * Common interface for all SonarLint objects that can have issues (=marker)
 * @since 3.0
 */
public interface ISonarLintIssuable {

  ISonarLintProject getProject();

  /**
   * Name to uniquely identify this object in the workspace.
   */
  String getName();

  /**
   * The resource where markers will be created
   */
  IResource getResource();

  /**
   * Resource name that will be displayed in marker properties.
   */
  default String getResourceNameForMarker() {
    return getResource().getName();
  }

  /**
   * Resource container that will be displayed in marker properties.
   */
  default String getResourceContainerForMarker() {
    var path = getResource().getFullPath();
    // n is the number of segments in container, not path
    var n = path.segmentCount() - 1;
    if (n <= 0) {
      return "";
    }
    var sb = new StringBuilder();
    for (var i = 0; i < n; ++i) {
      if (i != 0) {
        sb.append('/');
      }
      sb.append(path.segment(i));
    }
    return sb.toString();

  }

}
