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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.LogListener;

import static org.assertj.core.api.Assertions.assertThat;

public class DurationUtilsTest {
  private static final String PROPERTY_NULL = "PROPERTY_NULL";
  private static final String PROPERTY_MINUTES = "PROPERTY_MINUTES";
  private static final String PROPERTY_COMPLEX = "PROPERTY_COMPLEX";
  private static final String PROPERTY_FAILURE = "PROPERTY_FAILURE";

  private static final List<String> errors = new ArrayList<>();

  @BeforeClass
  public static void setSystemProperties() {
    System.setProperty(PROPERTY_MINUTES, "4");
    System.setProperty(PROPERTY_COMPLEX, "PT3H");
    System.setProperty(PROPERTY_FAILURE, "bamboozled");

    SonarLintLogger.get().addLogListener(new LogListener() {
      @Override
      public void info(@Nullable String msg, boolean fromAnalyzer) {
      }

      @Override
      public void error(@Nullable String msg, boolean fromAnalyzer) {
        errors.add(msg);
      }

      @Override
      public void debug(@Nullable String msg, boolean fromAnalyzer) {
      }

      @Override
      public void traceIdeMessage(@Nullable String msg) {
        // INFO: We ignore Eclipse-specific tracing in UTs
      }
    });
  }

  @AfterClass
  public static void removeSystemProperties() {
    System.clearProperty(PROPERTY_NULL);
    System.clearProperty(PROPERTY_MINUTES);
    System.clearProperty(PROPERTY_COMPLEX);
    System.clearProperty(PROPERTY_FAILURE);
  }

  @Test
  public void test_getTimeoutProperty_null() {
    assertThat(DurationUtils.getTimeoutProperty(PROPERTY_NULL))
      .isNull();
  }

  @Test
  public void test_getTimeoutProperty_simple() {
    assertThat(DurationUtils.getTimeoutProperty(PROPERTY_MINUTES))
      .isEqualTo(Duration.ofMinutes(4));
  }

  @Test
  public void test_getTimeoutProperty_complex() {
    assertThat(DurationUtils.getTimeoutProperty(PROPERTY_COMPLEX))
      .isEqualTo(Duration.ofHours(3));
  }

  @Test()
  public void test_getTimeoutProperty_fails() {
    assertThat(DurationUtils.getTimeoutProperty(PROPERTY_FAILURE))
      .isNull();
    assertThat(errors)
      .contains("Timeout of system property '" + PROPERTY_FAILURE + "' cannot be parsed!");
  }
}
