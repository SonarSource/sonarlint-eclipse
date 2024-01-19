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
package org.sonarlint.eclipse.core.internal.utils;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarlint.eclipse.core.internal.utils.StringUtils.isBlank;
import static org.sonarlint.eclipse.core.internal.utils.StringUtils.joinSkipNull;

public class StringUtilsTest {

  @Test
  public void testJoin() {
    assertThat(joinSkipNull(asList("a", "b", "c"), ",")).isEqualTo("a,b,c");
    assertThat(joinSkipNull(asList("a", null, "c"), ",")).isEqualTo("a,c");
    assertThat(joinSkipNull(asList(null, "b", "c"), ",")).isEqualTo("b,c");
    assertThat(joinSkipNull(asList(null, null, "c"), ",")).isEqualTo("c");
    assertThat(joinSkipNull(asList(null, null, null), ",")).isEmpty();
  }

  @Test
  public void testJoinWithComma() {
    assertThat(StringUtils.joinWithCommaSkipNull(asList("a", "b", "c"))).isEqualTo("a,b,c");
    assertThat(StringUtils.joinWithCommaSkipNull(asList("a", null, "c"))).isEqualTo("a,c");
    assertThat(StringUtils.joinWithCommaSkipNull(asList(null, "b", "c"))).isEqualTo("b,c");
    assertThat(StringUtils.joinWithCommaSkipNull(asList(null, null, "c"))).isEqualTo("c");
    assertThat(StringUtils.joinWithCommaSkipNull(asList(null, null, null))).isEmpty();
    assertThat(StringUtils.joinWithCommaSkipNull(asList(null, null, null))).isEmpty();
    assertThat(StringUtils.joinWithCommaSkipNull(asList("with,comma", "another,one", null, "no_comma"))).isEqualTo("\"with,comma\",\"another,one\",no_comma");
  }

  @Test
  public void testIsBlank() {
    assertThat(isBlank(null)).isTrue();
    assertThat(isBlank("")).isTrue();
    assertThat(isBlank("   ")).isTrue();
    assertThat(isBlank("\t \n")).isTrue();
    assertThat(isBlank("abc")).isFalse();
    assertThat(isBlank("ab c")).isFalse();
    assertThat(isBlank(" abc")).isFalse();
  }

}
