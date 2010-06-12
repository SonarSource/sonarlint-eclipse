package org.sonar.ide.eclipse.ui.tests;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Test;
import org.sonar.ide.eclipse.SonarUI;
import org.sonar.ide.eclipse.views.MeasuresView;


public class MeasuresTest extends UITestCase {

  @Test
  public void testShowMeasures() throws Exception {
    configureDefaultSonarServer(getTestServer().getBaseUrl());
    
    // Import project
    final String projectName = "measures";
    importMavenProject(projectName);
    
    // Open measures view
    openPerspective(SonarUI.ID_PERSPECTIVE);
    bot.viewById(MeasuresView.ID).setFocus();
    
    bot.toolbarToggleButtonWithTooltip("Link with Editor").click();
        
    SWTBotTable table = bot.table();
    assertThat(table.rowCount(), is(0));

    // Open file
    SWTBotTree tree = selectProject(projectName);
    SWTBotTreeItem treeItem = tree.expandNode(projectName, "src/main/java", "(default package)");
    treeItem.getNode("Measures.java").doubleClick();
    
    bot.sleep(1000 * 20);
    
    assertThat(table.rowCount(), greaterThan(0));
  }

}