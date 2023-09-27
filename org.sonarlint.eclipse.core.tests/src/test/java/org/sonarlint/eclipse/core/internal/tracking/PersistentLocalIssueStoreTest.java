/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2023 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.tracking;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.resources.DefaultSonarLintProjectAdapter;
import org.sonarlint.eclipse.tests.common.SonarTestCase;

import static org.assertj.core.api.Assertions.assertThat;

public class PersistentLocalIssueStoreTest extends SonarTestCase {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static IProject project;

  private PersistentLocalIssueStore underTest;

  private Path cacheFolder;

  @BeforeClass
  public static void importProject() throws Exception {
    project = importEclipseProject("SimpleProject");
    // Configure the project
    SonarLintCorePlugin.getInstance().getProjectConfigManager().load(new ProjectScope(project), "A Project");
  }

  @Before
  public void setUp() throws IOException {
    cacheFolder = temporaryFolder.newFolder().toPath();
    underTest = new PersistentLocalIssueStore(cacheFolder, new DefaultSonarLintProjectAdapter(project));
  }

  @Test
  public void should_persist_empty_issue_list() throws IOException {
    assertThat(cacheFolder).isEmptyDirectory();
    for (var i = 0; i < 10; i++) {
      underTest.save("file" + i, List.of());
    }

    try (var stream = Files.walk(cacheFolder)) {
      var paths = stream.filter(Files::isRegularFile).collect(Collectors.toList());
      assertThat(paths).hasSize(10 + 1 /* index.pb */);
    }
  }

  @Test
  public void should_return_null_for_file_never_analyzed() {
    var key = "nonexistent";
    assertThat(underTest.contains(key)).isFalse();
    assertThat(underTest.read(key)).isNull();
  }

  @Test
  public void should_contain_key_and_return_empty_for_file_with_no_issues() throws IOException {
    var key = "dummy file";
    underTest.save(key, List.of());
    assertThat(underTest.contains(key)).isTrue();
    assertThat(underTest.read(key)).isEmpty();
  }
}
