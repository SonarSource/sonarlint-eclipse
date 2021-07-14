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
package org.sonarlint.eclipse.core.internal.utils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
// Using JUnit native assumptions, tests were failing with AssertJ assumptions
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

public class SonarLintUtilsTest {

  @Test
  public void shouldReturnPidOnJdk9Plus() {
    assumeTrue(isJava9Plus());
    assertThat(SonarLintUtils.getPlatformPid()).isNotEmpty();
  }

  @Test
  
  public void shouldReturnEmptyVersionOnJdk8() {
    assumeFalse(isJava9Plus());
    assertThat(SonarLintUtils.getPlatformPid()).isEmpty();
  }

  private static boolean isJava9Plus() {
    try {
      return Class.forName("java.lang.Runtime").getMethod("version") != null;
    } catch(Throwable t) {
      return false;
    }
  }
}
