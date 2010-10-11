package org.sonar.ide.eclipse.ui.tests;

import org.junit.Test;
import org.sonar.ide.eclipse.ui.tests.bots.ConfigureProjectsWizardBot;
import org.sonar.ide.eclipse.ui.tests.bots.ImportProjectBot;
import org.sonar.ide.eclipse.ui.tests.bots.PackageExplorerBot;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ConfigureProjectTest extends UITestCase {
  private static final String PROJECT_NAME = "reference";

  @Test
  public void canAssociateWithSonar() throws Exception {
    new ImportProjectBot().setPath(getProjectPath(PROJECT_NAME)).finish();

    new PackageExplorerBot()
        .expandAndSelect(PROJECT_NAME)
        .clickContextMenu("Configure", "Associate with Sonar...");

    ConfigureProjectsWizardBot projectWizardBot = new ConfigureProjectsWizardBot();
    projectWizardBot.finish();
    assertThat(projectWizardBot.getStatus(), is(" empty GroupId for project '" + PROJECT_NAME + "'"));
    projectWizardBot.find();
    projectWizardBot.finish();
  }

}
