/*
 * Sonar Eclipse
 * Copyright (C) 2010-2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.ui.tests;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.ide.eclipse.ui.tests.bots.ImportProjectBot;
import org.sonar.ide.eclipse.ui.tests.bots.PackageExplorerBot;
import org.sonar.ide.eclipse.ui.tests.utils.ProjectUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.Assert.fail;

public class CompareWithSonarActionTest extends UITestCase {
  private static final String PROJECT_NAME = "reference";

  private static final String[] MENUBAR_PATH = {"Compare With", "Sonar server"};

  @BeforeClass
  public static void importProject() throws Exception {
    new ImportProjectBot().setPath(getProjectPath(PROJECT_NAME)).finish();

    // Enable Sonar nature
    ProjectUtils.configureProject(PROJECT_NAME);
  }

  /**
   * SONARIDE-208
   */
  @Test
  public void shouldNotCompareNewFile() throws Exception {
    // Create new file, which wasn't analysed by Sonar
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME);
    IFile file = project.getFile("src/main/java/NewFile.java");
    byte[] bytes = "File contents".getBytes();
    InputStream source = new ByteArrayInputStream(bytes);
    file.create(source, IResource.NONE, null);
    // Assert that dialog was shown
    new PackageExplorerBot()
        .expandAndSelect(PROJECT_NAME, "src/main/java", "(default package)", "NewFile.java")
        .clickContextMenu(MENUBAR_PATH);
    bot.shell("Not found").activate();
    bot.sleep(5000);
    bot.button("OK").click();
    // Remove file
    file.delete(true, null);
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
