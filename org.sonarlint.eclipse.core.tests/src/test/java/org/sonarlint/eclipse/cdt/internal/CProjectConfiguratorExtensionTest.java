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
package org.sonarlint.eclipse.cdt.internal;

import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.junit.Before;
import org.junit.Test;
import org.sonarlint.eclipse.core.internal.resources.DefaultSonarLintProjectAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CProjectConfiguratorExtensionTest {
  private CProjectConfiguratorExtension extension;

  @Before
  public void setUp() {
    extension = new CProjectConfiguratorExtension();
  }

  @Test
  public void should_configurate_projects_c_nature() throws CoreException {
    var project = mock(IProject.class);
    when(project.hasNature(CProjectNature.C_NATURE_ID)).thenReturn(true);
    assertThat(extension.canConfigure(new DefaultSonarLintProjectAdapter(project))).isTrue();
  }

}
