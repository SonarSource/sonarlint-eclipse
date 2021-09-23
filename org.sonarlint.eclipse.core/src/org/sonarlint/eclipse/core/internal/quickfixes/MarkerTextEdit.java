/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2021 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.quickfixes;

import org.eclipse.core.resources.IMarker;
import org.sonarlint.eclipse.core.internal.adapter.Adapters;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;

public class MarkerTextEdit {

  private final String newText;
  private final IMarker marker;

  public MarkerTextEdit(IMarker marker, String newText) {
    this.marker = marker;
    this.newText = newText;
  }

  public String getNewText() {
    return newText;
  }

  public IMarker getMarker() {
    return marker;
  }

  public boolean isValid() {
    return marker.exists() && Adapters.adapt(marker.getResource(), ISonarLintFile.class) != null;
  }

}
