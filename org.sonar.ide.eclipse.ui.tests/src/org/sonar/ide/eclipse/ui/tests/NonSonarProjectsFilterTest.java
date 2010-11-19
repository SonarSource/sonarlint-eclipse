/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.ui.tests;

import org.junit.Test;
import org.sonar.ide.eclipse.ui.tests.bots.ImportProjectBot;
import org.sonar.ide.eclipse.ui.tests.bots.PackageExplorerBot;
import org.sonar.ide.eclipse.ui.tests.utils.ProjectUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class NonSonarProjectsFilterTest extends UITestCase {
  private static final String PROJECT_NAME = "reference";

  @Test
  public void test() throws Exception {
    new ImportProjectBot().setPath(getProjectPath(PROJECT_NAME)).finish();

    PackageExplorerBot packageExplorerBot = new PackageExplorerBot();

    assertThat(packageExplorerBot.hasItems(), is(true));

    // Enable "Non-Sonar projects" filter
    packageExplorerBot
        .filters()
        .check("Non-Sonar projects")
        .ok();
    assertThat(packageExplorerBot.hasItems(), is(false));

    // Enable Sonar nature
    ProjectUtils.configureProject(PROJECT_NAME);

    assertThat(packageExplorerBot.hasItems(), is(true));
  }
}
