/*
 * SonarLint for Eclipse
 * Copyright (C) SonarSource Sàrl
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

public class SonarLintUtilsTest {

  @Test
  public void shouldReturnPid() {
    assertThat(SonarLintUtils.getPlatformPid()).isNotZero();
  }

  @Test
  public void shouldReturnEuUrlWhenRegionIsMissingOrEu() {
    assertThat(SonarLintUtils.getSonarCloudUrl(null)).isEqualTo("https://sonarcloud.io");
    assertThat(SonarLintUtils.getSonarCloudUrl("EU")).isEqualTo("https://sonarcloud.io");
  }

  @Test
  public void shouldReturnUsUrlForUsRegion() {
    assertThat(SonarLintUtils.getSonarCloudUrl("US")).isEqualTo("https://sonarqube.us");
    assertThat(SonarLintUtils.getSonarCloudUrl("us")).isEqualTo("https://sonarqube.us");
  }

  @Test
  public void shouldReturnAuUrlForAuRegion() {
    assertThat(SonarLintUtils.getSonarCloudUrl("AU")).isEqualTo("https://sonarqube.au");
    assertThat(SonarLintUtils.getSonarCloudUrl("au")).isEqualTo("https://sonarqube.au");
  }

  @Test
  public void shouldAllowOverridingAuUrl() {
    var property = "sonarlint.internal.sonarcloud.au.url";
    System.setProperty(property, "https://au-staging.example");
    try {
      assertThat(SonarLintUtils.getSonarCloudUrl("AU")).isEqualTo("https://au-staging.example");
    } finally {
      System.clearProperty(property);
    }
  }
}
