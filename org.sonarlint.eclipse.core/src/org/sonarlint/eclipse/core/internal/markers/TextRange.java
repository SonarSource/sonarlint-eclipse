/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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

import javax.annotation.CheckForNull;

public class TextRange {

  private final Integer startLine;
  private final Integer startLineOffset;
  private final Integer endLine;
  private final Integer endLineOffset;

  public TextRange(Integer line) {
    this(line, null, null, null);
  }

  public TextRange(Integer startLine, Integer startLineOffset, Integer endLine, Integer endLineOffset) {
    this.startLine = startLine;
    this.startLineOffset = startLineOffset;
    this.endLine = endLine;
    this.endLineOffset = endLineOffset;
  }

  @CheckForNull
  public Integer getStartLine() {
    return startLine;
  }

  @CheckForNull
  public Integer getStartLineOffset() {
    return startLineOffset;
  }

  @CheckForNull
  public Integer getEndLine() {
    return endLine;
  }

  @CheckForNull
  public Integer getEndLineOffset() {
    return endLineOffset;
  }
}
