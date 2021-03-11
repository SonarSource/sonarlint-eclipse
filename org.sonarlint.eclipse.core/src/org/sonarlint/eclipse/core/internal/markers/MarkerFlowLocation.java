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
package org.sonarlint.eclipse.core.internal.markers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.annotation.Nullable;

public class MarkerFlowLocation {
  private final MarkerFlow parent;
  private final int number;
  @Nullable
  private final String message;
  @Nullable
  private IMarker marker;
  private boolean deleted;
  @Nullable
  private String filePath;

  public MarkerFlowLocation(MarkerFlow parent, @Nullable String message) {
    this.parent = parent;
    this.parent.locations.add(this);
    this.number = this.parent.locations.size();
    this.message = message;
  }

  public MarkerFlowLocation(MarkerFlow parent, @Nullable String message, @Nullable String filePath) {
    this(parent, message);
    this.filePath = filePath;
  }

  public MarkerFlow getParent() {
    return parent;
  }

  public int getNumber() {
    return number;
  }

  @Nullable
  public String getMessage() {
    return message;
  }

  public void setMarker(IMarker marker) {
    this.marker = marker;
  }

  @Nullable
  public IMarker getMarker() {
    return marker;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  @Nullable
  public String getFilePath() {
    return filePath;
  }
}
