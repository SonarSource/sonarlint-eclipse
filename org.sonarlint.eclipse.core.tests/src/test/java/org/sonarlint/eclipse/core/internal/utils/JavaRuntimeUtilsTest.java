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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaRuntimeUtilsTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void returns_empty_when_release_file_is_absent() throws IOException {
    var dir = temporaryFolder.newFolder().toPath();
    assertThat(JavaRuntimeUtils.getJavaMajorVersion(dir)).isEmpty();
  }

  @Test
  public void returns_empty_when_release_file_has_no_java_version_line() throws IOException {
    var dir = releaseFileWith("OS_NAME=\"Linux\"\nOS_VERSION=\"5.15\"\n");
    assertThat(JavaRuntimeUtils.getJavaMajorVersion(dir)).isEmpty();
  }

  @Test
  public void returns_empty_when_version_string_is_malformed() throws IOException {
    var dir = releaseFileWithVersion("not-a-version");
    assertThat(JavaRuntimeUtils.getJavaMajorVersion(dir)).isEmpty();
  }

  @Test
  public void parses_modern_version_with_patch() throws IOException {
    assertThat(JavaRuntimeUtils.getJavaMajorVersion(releaseFileWithVersion("21.0.1"))).hasValue(21);
  }

  @Test
  public void parses_modern_version_without_minor() throws IOException {
    assertThat(JavaRuntimeUtils.getJavaMajorVersion(releaseFileWithVersion("21"))).hasValue(21);
  }

  @Test
  public void parses_java_17() throws IOException {
    assertThat(JavaRuntimeUtils.getJavaMajorVersion(releaseFileWithVersion("17.0.9"))).hasValue(17);
  }

  @Test
  public void parses_java_11() throws IOException {
    assertThat(JavaRuntimeUtils.getJavaMajorVersion(releaseFileWithVersion("11.0.22"))).hasValue(11);
  }

  @Test
  public void parses_legacy_java_8_version_format() throws IOException {
    assertThat(JavaRuntimeUtils.getJavaMajorVersion(releaseFileWithVersion("1.8.0_382"))).hasValue(8);
  }

  @Test
  public void ignores_unrelated_lines_around_java_version() throws IOException {
    var dir = releaseFileWith(
      "OS_NAME=\"Linux\"\n"
        + "JAVA_VERSION=\"21.0.1\"\n"
        + "IMPLEMENTOR=\"Eclipse Adoptium\"\n"
        + "JAVA_RUNTIME_VERSION=\"21.0.1+12\"\n");
    assertThat(JavaRuntimeUtils.getJavaMajorVersion(dir)).hasValue(21);
  }

  @Test
  public void detects_java_executable_on_unix() throws IOException {
    var dir = temporaryFolder.newFolder().toPath();
    var bin = Files.createDirectory(dir.resolve("bin"));
    Files.createFile(bin.resolve("java"));

    assertThat(JavaRuntimeUtils.checkForJavaExecutable(dir)).isTrue();
  }

  @Test
  public void returns_false_when_executable_is_missing() throws IOException {
    var dir = temporaryFolder.newFolder().toPath();
    Files.createDirectory(dir.resolve("bin"));

    assertThat(JavaRuntimeUtils.checkForJavaExecutable(dir)).isFalse();
  }

  private Path releaseFileWithVersion(String version) throws IOException {
    return releaseFileWith("JAVA_VERSION=\"" + version + "\"\n");
  }

  private Path releaseFileWith(String content) throws IOException {
    var dir = temporaryFolder.newFolder().toPath();
    Files.writeString(dir.resolve("release"), content);
    return dir;
  }
}
