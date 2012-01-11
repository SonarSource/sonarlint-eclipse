/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.api;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Evgeny Mandrikov
 * @since 0.2
 */
public final class SourceCodeDiff {

  public static final int NOT_FOUND = -1;

  private Map<Integer, Integer> diff = new HashMap<Integer, Integer>();

  public SourceCodeDiff() {
  }

  /**
   * @param oldLine line in Sonar server (starting from 1)
   * @param newLine line in working copy (starting from 0), -1 if not found
   */
  public void map(int oldLine, int newLine) {
    if (newLine != NOT_FOUND) {
      diff.put(oldLine, newLine);
    }
  }

  /**
   * @param oldLine line in Sonar server (starting from 1)
   * @return line in working copy (starting from 0), -1 if not found
   */
  public Integer newLine(int oldLine) {
    if (diff.containsKey(oldLine)) {
      return diff.get(oldLine);
    }
    return NOT_FOUND;
  }

  @Override
  public String toString() {
    return diff.toString();
  }
}
