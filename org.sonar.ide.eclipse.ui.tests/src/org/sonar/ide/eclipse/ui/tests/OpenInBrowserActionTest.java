package org.sonar.ide.eclipse.ui.tests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.ide.eclipse.ui.tests.bots.ImportProjectBot;
import org.sonar.ide.eclipse.ui.tests.bots.PackageExplorerBot;
import org.sonar.ide.eclipse.ui.tests.utils.ProjectUtils;

public class OpenInBrowserActionTest extends UITestCase {
  private static final String PROJECT_NAME = "reference";

  private static final String WEB_BROWSER_EDITOR_ID = "org.eclipse.ui.browser.editor";
  private static final String[] MENUBAR_PATH = { "Sonar", "Open in Sonar server" };

  @BeforeClass
  public static void importProject() throws Exception {
    new ImportProjectBot().setPath(getProjectPath(PROJECT_NAME)).finish();

    // Enable Sonar nature
    ProjectUtils.configureProject(PROJECT_NAME);
  }

  @Test
  public void canOpenFile() {
    new PackageExplorerBot()
        .expandAndSelect(PROJECT_NAME, "src/main/java", "(default package)", "ClassOnDefaultPackage.java")
        .clickContextMenu(MENUBAR_PATH);
    bot.editorById(WEB_BROWSER_EDITOR_ID);
    bot.closeAllEditors();
  }

  @Test
  public void canOpenPackage() {
    new PackageExplorerBot()
        .expandAndSelect(PROJECT_NAME, "src/main/java", "(default package)")
        .clickContextMenu(MENUBAR_PATH);
    bot.editorById(WEB_BROWSER_EDITOR_ID);
    bot.closeAllEditors();
  }

  @Test
  public void canOpenProject() {
    new PackageExplorerBot()
        .expandAndSelect(PROJECT_NAME)
        .clickContextMenu(MENUBAR_PATH);
    bot.editorById(WEB_BROWSER_EDITOR_ID);
    bot.closeAllEditors();
  }
}
