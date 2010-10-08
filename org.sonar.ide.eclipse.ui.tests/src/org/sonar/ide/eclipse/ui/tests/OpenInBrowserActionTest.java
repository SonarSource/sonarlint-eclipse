package org.sonar.ide.eclipse.ui.tests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.ui.tests.bots.ImportProjectBot;
import org.sonar.ide.eclipse.ui.tests.bots.PackageExplorerBot;
import org.sonar.ide.eclipse.ui.tests.utils.ProjectUtils;

public class OpenInBrowserActionTest extends UITestCase {
  private static final String WEB_BROWSER_EDITOR_ID = "org.eclipse.ui.browser.editor";
  private static final String[] MENUBAR_PATH = { "Sonar", "Open in Sonar server" };

  /**
   * Workaround for accessing non static method {@link #getSonarServerUrl()} from static method {@link #importProject()}
   */
  static class TTT extends UITestCase {
  }

  @BeforeClass
  public static void importProject() throws Exception {
    String url = new TTT().getSonarServerUrl();
    SonarPlugin.getServerManager().findServer(url);

    new ImportProjectBot().setPath(getProject("SimpleProject").getCanonicalPath()).finish();

    ProjectUtils.configureProject("SimpleProject", url);
  }

  @Test
  public void canOpenFile() {
    new PackageExplorerBot()
        .expandAndSelect("SimpleProject", "src/main/java", "(default package)", "ViolationOnFile.java")
        .clickContextMenu(MENUBAR_PATH);
    bot.editorById(WEB_BROWSER_EDITOR_ID);
    bot.closeAllEditors();
  }

  @Test
  public void canOpenPackage() {
    new PackageExplorerBot()
        .expandAndSelect("SimpleProject", "src/main/java", "(default package)")
        .clickContextMenu(MENUBAR_PATH);
    bot.editorById(WEB_BROWSER_EDITOR_ID);
    bot.closeAllEditors();
  }

  @Test
  public void canOpenProject() {
    new PackageExplorerBot()
        .expandAndSelect("SimpleProject")
        .clickContextMenu(MENUBAR_PATH);
    bot.editorById(WEB_BROWSER_EDITOR_ID);
    bot.closeAllEditors();
  }
}
