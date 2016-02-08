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
package org.sonarlint.eclipse.core.internal.utils;

import java.util.Arrays;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StringUtilsTest {

  @Test
  public void testJoin() {
    assertThat(StringUtils.joinSkipNull(Arrays.asList("a", "b", "c"), ",")).isEqualTo("a,b,c");
    assertThat(StringUtils.joinSkipNull(Arrays.asList("a", null, "c"), ",")).isEqualTo("a,c");
    assertThat(StringUtils.joinSkipNull(Arrays.asList(null, "b", "c"), ",")).isEqualTo("b,c");
    assertThat(StringUtils.joinSkipNull(Arrays.asList(null, null, "c"), ",")).isEqualTo("c");
    assertThat(StringUtils.joinSkipNull(Arrays.<String>asList(null, null, null), ",")).isEqualTo("");
  }

}
