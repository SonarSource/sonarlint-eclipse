/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.regex.Pattern;
import org.eclipse.jdt.annotation.Nullable;

import static java.util.stream.Collectors.joining;

public class StringUtils {
  private static String lineSeperator = System.lineSeparator();
  public static final String EMPTY = "";
  public static final int INDEX_NOT_FOUND = -1;
  private static final String COMMA_SEPARATOR = ",";

  private StringUtils() {
  }

  public static boolean isBlank(@Nullable String str) {
    int strLen;
    if (str == null || (strLen = str.length()) == 0) {
      return true;
    }
    for (var i = 0; i < strLen; i++) {
      if (!Character.isWhitespace(str.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  public static boolean isNotBlank(@Nullable String str) {
    return !StringUtils.isBlank(str);
  }

  public static boolean isEmpty(@Nullable String str) {
    return str == null || str.isEmpty();
  }

  @Nullable
  public static String substringAfterLast(@Nullable String str, @Nullable String separator) {
    if (isEmpty(str)) {
      return str;
    }
    if (isEmpty(separator)) {
      return EMPTY;
    }
    var pos = str.lastIndexOf(separator);
    if (pos == INDEX_NOT_FOUND || pos == (str.length() - separator.length())) {
      return EMPTY;
    }
    return str.substring(pos + separator.length());
  }

  public static String joinSkipNull(Collection<String> values, String separator) {
    return values.stream()
      .filter(Objects::nonNull)
      .collect(joining(separator));
  }

  public static String joinWithCommaSkipNull(Collection<String> values) {
    return values.stream()
      .filter(Objects::nonNull)
      .map(StringUtils::csvEscape)
      .collect(joining(COMMA_SEPARATOR));
  }

  public static Collection<String> splitFromCommaString(String list) {
    return Arrays.asList(list.split(COMMA_SEPARATOR));
  }

  private static String csvEscape(String string) {
    // escape only when needed
    return string.contains(",") ? ("\"" + string + "\"") : string;
  }

  public static String[] split(@Nullable String str, String separator) {
    return isEmpty(str) ? (new String[0]) : str.split(Pattern.quote(separator));
  }

  public static String defaultString(@Nullable String str, String defaultStr) {
    return str == null ? defaultStr : str;
  }

  public static String defaultString(@Nullable String str) {
    return str == null ? EMPTY : str;
  }

  @Nullable
  public static String removeEnd(@Nullable String str, @Nullable String remove) {
    if (isEmpty(str) || isEmpty(remove)) {
      return str;
    }
    if (str.endsWith(remove)) {
      return str.substring(0, str.length() - remove.length());
    }
    return str;
  }

  public static <T extends CharSequence> T defaultIfBlank(@Nullable final T str, final T defaultStr) {
    return isBlank(str) ? defaultStr : str;
  }

  public static boolean isBlank(@Nullable final CharSequence cs) {
    int strLen;
    if (cs == null || (strLen = cs.length()) == 0) {
      return true;
    }
    for (var i = 0; i < strLen; i++) {
      if (!Character.isWhitespace(cs.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  public static String trimToEmpty(@Nullable final String str) {
    return str == null ? EMPTY : str.trim();
  }

  @Nullable
  public static String trimToNull(@Nullable final String str) {
    final var trimmed = trim(str);
    return isEmpty(trimmed) ? null : trimmed;
  }

  @Nullable
  public static String trim(@Nullable final String str) {
    return str == null ? null : str.trim();
  }

  @Nullable
  public static String substringBefore(@Nullable final String str, @Nullable final String separator) {
    if (isEmpty(str) || separator == null) {
      return str;
    }
    if (separator.isEmpty()) {
      return EMPTY;
    }
    final var pos = str.indexOf(separator);
    if (pos == INDEX_NOT_FOUND) {
      return str;
    }
    return str.substring(0, pos);
  }

  public static String capitalize(String s) {
    if (s.isEmpty()) {
      return s;
    }
    return Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }

  public static String urlEncode(String toEncode) {
    try {
      return URLEncoder.encode(toEncode, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Should never happen", e);
    }
  }

  public static String urlDecode(String toDecode) {
    try {
      return URLDecoder.decode(toDecode, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Should never happen", e);
    }
  }

  public static String xmlDecode(String toDecode) {
    return toDecode.replace("&quot;", "\"").replace("&apos;", "\'").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
  }

  /**
   *  When having a (possible multi-line) string and a (possible multi-line) substring, and the starting line of the
   *  substring is required, this can be used to calculate it.
   *
   *  @param str that can contain subString
   *  @param subString that is searched for
   *  @return the line of the subString inside the main string
   */
  public static int getLineOfSubstring(final String str, final String subString) {
    var subStringIndex = str.indexOf(subString);
    if (subStringIndex == -1) {
      return subStringIndex;
    }

    // In order to not have an ArithmeticException down below if there is no new line in the whole text!
    if (!str.contains(lineSeperator)) {
      return 0;
    }

    var preSubString = str.substring(0, subStringIndex);
    return getNumberOfLines(preSubString);
  }

  public static int getNumberOfLines(final String str) {
    return str.split(lineSeperator).length;
  }
}
