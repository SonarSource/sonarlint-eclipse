/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2020 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.notifications;

import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.tests.common.SonarTestCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NotificationsTrackerRegistryTest extends SonarTestCase {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void should_create_one_tracker_per_project() throws IOException {
    NotificationsTrackerRegistry registry = new NotificationsTrackerRegistry();
    String name = "dummy project name";
    NotificationsTracker tracker = registry.getOrCreate(createProjectWithName(name));
    assertThat(registry.getOrCreate(createProjectWithName(name))).isEqualTo(tracker);
    assertThat(registry.getOrCreate(createProjectWithName(name + "-foo"))).isNotEqualTo(tracker);
  }

  private ISonarLintProject createProjectWithName(String name) throws IOException {
    ISonarLintProject project = mock(ISonarLintProject.class);
    when(project.getName()).thenReturn(name);
    when(project.getWorkingDir()).thenReturn(temp.newFolder().toPath());
    return project;
  }
}
