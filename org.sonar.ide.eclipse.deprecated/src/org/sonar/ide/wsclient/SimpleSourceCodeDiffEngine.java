/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2011 SonarSource
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
package org.sonar.ide.wsclient;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ide.api.SourceCodeDiff;
import org.sonar.ide.api.SourceCodeDiffEngine;
import org.sonar.wsclient.services.Source;

/**
 * Actually this is an implementation of heuristic algorithm - magic happens here.
 *
 * @author Evgeny Mandrikov
 */
public class SimpleSourceCodeDiffEngine implements SourceCodeDiffEngine {
  private static final Logger LOG = LoggerFactory.getLogger(SimpleSourceCodeDiffEngine.class);

  public static SourceCodeDiffEngine getInstance() {
    return new SimpleSourceCodeDiffEngine();
  }

  public SourceCodeDiff diff(String local, String remote) {
    return diff(split(local), split(remote));
  }

  /**
   * {@inheritDoc}
   */
  public SourceCodeDiff diff(String[] local, String remote[]) {
    SourceCodeDiff result = new SourceCodeDiff();

    int[] hashCodes = getHashCodes(local);
    // Works for O(S*L) time, where S - number of lines on server and L - number of lines in working copy.
    for (int i = 0; i < remote.length; i++) {
      int originalLine = i + 1;
      int newLine = internalMatch(remote[i], hashCodes, originalLine);
      if (newLine != -1) {
        result.map(originalLine, newLine);
      }
    }

    return result;
  }

  /**
   * Currently this method just compares hash codes (see {@link #getHashCode(String)}).
   *
   * @return -1 if not found
   */
  private int internalMatch(String originalSourceLine, int[] hashCodes, int originalLine) {
    int newLine = -1;
    int originalHashCode = getHashCode(originalSourceLine);
    // line might not exists in working copy
    if (originalLine - 1 < hashCodes.length) {
      if (hashCodes[originalLine - 1] == originalHashCode) {
        newLine = originalLine;
      }
    }
    for (int i = 0; i < hashCodes.length; i++) {
      if (hashCodes[i] == originalHashCode) {
        if (newLine != -1 && newLine != originalLine) {
          // may be more than one match, but we take into account only first
          LOG.warn("Found more than one match for line '{}'", originalSourceLine);
          break;
        }
        newLine = i + 1;
      }
    }
    return newLine;
  }

  public static String[] split(String text) {
    return StringUtils.splitPreserveAllTokens(text, '\n');
  }

  /**
   * Returns hash code for specified string after removing whitespaces.
   *
   * @param str string
   * @return hash code for specified string after removing whitespaces
   */
  static int getHashCode(String str) {
    if (str == null) {
      return 0;
    }
    return StringUtils.deleteWhitespace(str).hashCode();
  }

  /**
   * Returns hash codes for specified strings after removing whitespaces.
   *
   * @param str strings
   * @return hash codes for specified strings after removing whitespaces
   */
  private static int[] getHashCodes(String[] str) {
    int[] hashCodes = new int[str.length];
    for (int i = 0; i < str.length; i++) {
      hashCodes[i] = getHashCode(str[i]);
    }
    return hashCodes;
  }

  public static String[] getLines(Source source) {
    String[] remote = new String[source.getLinesById().lastKey()];
    for (int i = 0; i < remote.length; i++) {
      remote[i] = source.getLine(i + 1);
      if (remote[i] == null) {
        remote[i] = "";
      }
    }
    return remote;
  }

}
