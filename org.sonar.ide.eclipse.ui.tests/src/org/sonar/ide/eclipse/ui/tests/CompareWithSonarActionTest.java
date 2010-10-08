package org.sonar.ide.eclipse.ui.tests;

import static org.junit.Assert.fail;

import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.ui.tests.bots.ImportProjectBot;
import org.sonar.ide.eclipse.ui.tests.bots.PackageExplorerBot;
import org.sonar.ide.eclipse.ui.tests.utils.ProjectUtils;

public class CompareWithSonarActionTest extends UITestCase {
  private static final String[] MENUBAR_PATH = { "Compare With", "Sonar server" };

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
  public void canCompareFile() {
    new PackageExplorerBot()
        .expandAndSelect("SimpleProject", "src/main/java", "(default package)", "ViolationOnFile.java")
        .clickContextMenu(MENUBAR_PATH);
    bot.editorByTitle("Compare");
    bot.closeAllEditors();
  }

  @Test
  public void cantComparePackage() {
    PackageExplorerBot packageExplorerBot = new PackageExplorerBot()
        .expandAndSelect("SimpleProject", "src/main/java", "(default package)");
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
        .expandAndSelect("SimpleProject");
    try {
      packageExplorerBot.clickContextMenu(MENUBAR_PATH);
    } catch (WidgetNotFoundException e) {
      return;
    }
    fail("Possible to compare project with Sonar");
  }
}
