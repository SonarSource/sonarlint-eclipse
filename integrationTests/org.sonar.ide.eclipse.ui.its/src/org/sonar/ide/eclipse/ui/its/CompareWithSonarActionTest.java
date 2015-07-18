/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonar.ide.eclipse.ui.its;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.ide.eclipse.ui.its.bots.ImportProjectBot;
import org.sonar.ide.eclipse.ui.its.bots.JavaPackageExplorerBot;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.Assert.fail;

public class CompareWithSonarActionTest extends AbstractSQEclipseUITest {
  private static final String PROJECT_NAME = "reference";

  private static final String[] MENUBAR_PATH = {"Compare With", "SonarQube server"};

  @BeforeClass
  public static void importProject() throws Exception {
    new ImportProjectBot(bot).setPath(getProjectPath(PROJECT_NAME)).finish();

    // Enable Sonar nature
    configureProject(PROJECT_NAME);
  }

  /**
   * SONARIDE-208
   */
  @Test
  public void shouldNotCompareNewFile() throws Exception {
    // Create new file, which wasn't analysed by Sonar
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME);
    final IFile file = project.getFile("src/main/java/NewFile.java");
    byte[] bytes = "File contents".getBytes();
    InputStream source = new ByteArrayInputStream(bytes);
    try {
      file.create(source, IResource.NONE, new NullProgressMonitor() {
        @Override
        public void done() {
          // Assert that dialog was shown
          new JavaPackageExplorerBot(bot)
            .expandAndSelect(PROJECT_NAME, "src/main/java", "(default package)", "NewFile.java")
            .clickContextMenu(MENUBAR_PATH);
          bot.shell("Not found").activate();
          bot.sleep(1000);
          bot.button("OK").click();
        }
      });
    } finally {
      // Remove file quietly
      try {
        file.delete(true, null);
      } catch (Exception e) {
        // Ignore
      }
    }

  }

  @Test
  public void canCompareFile() {
    new JavaPackageExplorerBot(bot)
      .expandAndSelect(PROJECT_NAME, "src/main/java", "(default package)", "ClassOnDefaultPackage.java")
      .clickContextMenu(MENUBAR_PATH);
    bot.editorByTitle("Compare");
    bot.closeAllEditors();
  }

  @Test
  public void cantComparePackage() {
    JavaPackageExplorerBot packageExplorerBot = new JavaPackageExplorerBot(bot)
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
    JavaPackageExplorerBot packageExplorerBot = new JavaPackageExplorerBot(bot)
      .expandAndSelect(PROJECT_NAME);
    try {
      packageExplorerBot.clickContextMenu(MENUBAR_PATH);
    } catch (WidgetNotFoundException e) {
      return;
    }
    fail("Possible to compare project with Sonar");
  }
}
