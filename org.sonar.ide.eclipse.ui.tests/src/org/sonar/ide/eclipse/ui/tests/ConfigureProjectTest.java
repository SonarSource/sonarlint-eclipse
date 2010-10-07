package org.sonar.ide.eclipse.ui.tests;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.Before;
import org.junit.Test;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.ui.tests.bots.ConfigureProjectsWizardBot;
import org.sonar.ide.eclipse.ui.tests.utils.ContextMenuHelper;
import org.sonar.ide.eclipse.ui.tests.utils.SwtBotUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ConfigureProjectTest extends UITestCase {
  private ConfigureProjectsWizardBot projectWizardBot;

  @Before
  public void importProject() throws Exception {
    SonarPlugin.getServerManager().findServer(getSonarServerUrl());

    importNonMavenProject("SimpleProject");
    SWTBotTree project = SwtBotUtils.selectProject(bot, "SimpleProject");
    ContextMenuHelper.clickContextMenu(project, "Configure", "Associate with Sonar...");
    projectWizardBot = new ConfigureProjectsWizardBot();
  }

  @Test
  public void canAssociateWithSonar() throws Exception {
    projectWizardBot.finish();
    assertThat(projectWizardBot.getStatus(), is(" empty GroupId for project 'SimpleProject'"));
    projectWizardBot.find();
    projectWizardBot.cancel();
  }

}
