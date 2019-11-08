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
package org.sonarlint.eclipse.core.internal.markers;

import javax.annotation.Nullable;

public interface TextRange {

  default boolean isValid() {
    return false;
  }

  default boolean isLineOnly() {
    return false;
  }

  default int getStartLine() {
    throw new UnsupportedOperationException();
  }

  default int getStartLineOffset() {
    throw new UnsupportedOperationException();
  }

  default int getEndLine() {
    throw new UnsupportedOperationException();
  }

  default int getEndLineOffset() {
    throw new UnsupportedOperationException();
  }

  static TextRange get(@Nullable Integer line) {
    if (line == null) {
      return new InvalidTextRange();
    } else {
      return new LineTextRange(line);
    }
  }

  static TextRange get(@Nullable Integer startLine, @Nullable Integer startLineOffset, @Nullable Integer endLine, @Nullable Integer endLineOffset) {
    if (startLine == null || startLineOffset == null || endLine == null || endLineOffset == null) {
      return get(startLine);
    } else {
      return new FullTextRange(startLine, startLineOffset, endLine, endLineOffset);
    }
  }

  public static class InvalidTextRange implements TextRange {}

  public static class LineTextRange implements TextRange {

    private int startLine;

    private LineTextRange (int startLine) {
      this.startLine = startLine;
    }

    @Override
    public boolean isValid() {
      return true;
    }

    @Override
    public boolean isLineOnly() {
      return true;
    }

    @Override
    public int getStartLine() {
      return startLine;
    }
  }

  public static class FullTextRange implements TextRange {
    private final int startLine;
    private final int startLineOffset;
    private final int endLine;
    private final int endLineOffset;

    private FullTextRange(int startLine, int startLineOffset, int endLine, int endLineOffset) {
      this.startLine = startLine;
      this.startLineOffset = startLineOffset;
      this.endLine = endLine;
      this.endLineOffset = endLineOffset;
    }

    @Override
    public boolean isValid() {
      return true;
    }

    @Override
    public int getStartLine() {
      return startLine;
    }

    @Override
    public int getStartLineOffset() {
      return startLineOffset;
    }

    @Override
    public int getEndLine() {
      return endLine;
    }

    @Override
    public int getEndLineOffset() {
      return endLineOffset;
    }
  }
}
