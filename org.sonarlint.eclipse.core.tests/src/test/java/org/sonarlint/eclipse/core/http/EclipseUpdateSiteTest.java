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
package org.sonarlint.eclipse.core.http;

import org.junit.Test;
import org.sonarlint.eclipse.core.internal.http.EclipseUpdateSite;

import static org.assertj.core.api.Assertions.assertThat;

public class EclipseUpdateSiteTest {
  @Test
  public void test_parseXmlIntoSonarLintVersion_incorrect() {
    assertThat(EclipseUpdateSite.parseXmlIntoSonarLintVersion("")).isNull();
    assertThat(EclipseUpdateSite.parseXmlIntoSonarLintVersion(
      "<child location=\"https://binaries.sonarsource.com/SonarLint-for-Eclipse/releases/\"/>"))
        .isNull();
    assertThat(EclipseUpdateSite.parseXmlIntoSonarLintVersion(
      "<child location=\"1\"/><child location=\"2\"/>"))
        .isNull();
  }

  @Test
  public void test_parseXmlIntoSonarLintVersion_correct() {
    var version1 = EclipseUpdateSite.parseXmlIntoSonarLintVersion(
      "<child location=\"https://binaries.sonarsource.com/SonarLint-for-Eclipse/releases/10.4.0.82051/\"/>");
    assertThat(version1.major).isEqualTo(10);
    assertThat(version1.minor).isEqualTo(4);
    assertThat(version1.patch).isZero();

    var version2 = EclipseUpdateSite.parseXmlIntoSonarLintVersion(
      "<child location=\"/10.4.0.82051/\"/><child location=\"/10.3.2.82051/\"/><child location=\"/10.2.1.82051/\"/>");
    assertThat(version2.major).isEqualTo(10);
    assertThat(version2.minor).isEqualTo(4);
    assertThat(version2.patch).isZero();
  }

  @Test
  public void test_getEclipseUpdateSiteContent() {
    var xml = EclipseUpdateSite.getEclipseUpdateSiteContent();
    assertThat(xml).containsAnyOf(
      "org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository",
      "org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository");
  }

  @Test
  public void test_getNewestVersion() {
    assertThat(EclipseUpdateSite.getNewestVersion()).isNotNull();
  }
}
