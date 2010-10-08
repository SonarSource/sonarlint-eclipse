package org.sonar.ide.eclipse.ui.tests;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Before;
import org.junit.Test;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.ui.tests.bots.ConfigureProjectsWizardBot;
import org.sonar.ide.eclipse.ui.tests.bots.ImportProjectBot;
import org.sonar.ide.eclipse.ui.tests.bots.PackageExplorerBot;

public class ConfigureProjectTest extends UITestCase {
  private ConfigureProjectsWizardBot projectWizardBot;

  @Before
  public void importProject() throws Exception {
    SonarPlugin.getServerManager().findServer(getSonarServerUrl());

    new ImportProjectBot().setPath(getProject("SimpleProject").getCanonicalPath()).finish();

    new PackageExplorerBot()
        .expandAndSelect("SimpleProject")
        .clickContextMenu("Configure", "Associate with Sonar...");
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
