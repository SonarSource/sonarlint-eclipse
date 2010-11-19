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

import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.ide.eclipse.ui.tests.bots.ImportProjectBot;
import org.sonar.ide.eclipse.ui.tests.bots.PackageExplorerBot;
import org.sonar.ide.eclipse.ui.tests.utils.ProjectUtils;

import static org.junit.Assert.fail;

public class CompareWithSonarActionTest extends UITestCase {
  private static final String PROJECT_NAME = "reference";

  private static final String[] MENUBAR_PATH = { "Compare With", "Sonar server" };

  @BeforeClass
  public static void importProject() throws Exception {
    new ImportProjectBot().setPath(getProjectPath(PROJECT_NAME)).finish();

    // Enable Sonar nature
    ProjectUtils.configureProject(PROJECT_NAME);
  }

  @Test
  public void canCompareFile() {
    new PackageExplorerBot()
        .expandAndSelect(PROJECT_NAME, "src/main/java", "(default package)", "ClassOnDefaultPackage.java")
        .clickContextMenu(MENUBAR_PATH);
    bot.editorByTitle("Compare");
    bot.closeAllEditors();
  }

  @Test
  public void cantComparePackage() {
    PackageExplorerBot packageExplorerBot = new PackageExplorerBot()
        .expandAndSelect(PROJECT_NAME, "src/main/java", "(default package)");
    try {
      packageExplorerBot.clickContextMenu(MENUBAR_PATH);
    } catch (WidgetNotFoundException e) {
      return;
    }
    fail("Possible to compare package with Sonar");
  }

  @Test
  public void cantCompareProject() {
    PackageExplorerBot packageExplorerBot = new PackageExplorerBot()
        .expandAndSelect(PROJECT_NAME);
    try {
      packageExplorerBot.clickContextMenu(MENUBAR_PATH);
    } catch (WidgetNotFoundException e) {
      return;
    }
    fail("Possible to compare project with Sonar");
  }
}
