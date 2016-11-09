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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonarlint.eclipse.core.internal.utils.SonarLintUtils.isParentOf;
import static org.sonarlint.eclipse.core.internal.utils.SonarLintUtils.removeAggregatedDuplicates;

public class SonarLintUtilsTest {
  private IProject mockProject(String location) {
    IProject project = mock(IProject.class);
    when(project.getLocation()).thenReturn(new Path(location));
    return project;
  }

  private IFile mockFile() {
    IFile file = mock(IFile.class);
    return file;
  }

  @Test
  public void test_isParentOf() {
    IProject parent = mockProject("L/parent");
    IProject child1 = mockProject("L/parent/child1");
    IProject child2 = mockProject("L/parent/child2");

    assertThat(isParentOf(parent, child1)).isTrue();
    assertThat(isParentOf(child1, parent)).isFalse();
    assertThat(isParentOf(child1, child2)).isFalse();
  }

  @Test
  public void should_do_nothing_if_files_are_unrelated() {
    Map<IProject, Collection<IFile>> map = new HashMap<>();
    IFile file = mockFile();
    map.put(mockProject("L/parent/child1"), Arrays.asList(file));
    map.put(mockProject("L/parent/child1-notsubdir"), Arrays.asList(file));

    removeAggregatedDuplicates(map);
    assertThat(map.values().stream().map(v -> v.size())).containsExactly(1, 1);
  }

  @Test
  public void should_remove_file_from_parent_fileset() {
    Map<IProject, Collection<IFile>> map = new HashMap<>();
    IFile file = mockFile();
    IProject parent = mockProject("L/parent");
    IProject child1 = mockProject("L/parent/child1");
    IProject child2 = mockProject("L/parent/child2");
    map.put(parent, new ArrayList<>(Arrays.asList(file)));
    map.put(child1, Arrays.asList(file));
    map.put(child2, Arrays.asList(file));

    removeAggregatedDuplicates(map);
    assertThat(map.get(child1)).hasSize(1);
    assertThat(map.get(child2)).hasSize(1);
    assertThat(map.get(parent)).isEmpty();
  }
}
