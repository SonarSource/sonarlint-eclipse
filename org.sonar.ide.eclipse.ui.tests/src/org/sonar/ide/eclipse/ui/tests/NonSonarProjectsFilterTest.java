package org.sonar.ide.eclipse.ui.tests;

import org.junit.Test;
import org.sonar.ide.eclipse.ui.tests.bots.ImportProjectBot;
import org.sonar.ide.eclipse.ui.tests.bots.PackageExplorerBot;
import org.sonar.ide.eclipse.ui.tests.utils.ProjectUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class NonSonarProjectsFilterTest extends UITestCase {
  private static final String PROJECT_NAME = "reference";

  @Test
  public void test() throws Exception {
    new ImportProjectBot().setPath(getProjectPath(PROJECT_NAME)).finish();

    PackageExplorerBot packageExplorerBot = new PackageExplorerBot();

    assertThat(packageExplorerBot.hasItems(), is(true));

    // Enable "Non-Sonar projects" filter
    packageExplorerBot
        .filters()
        .check("Non-Sonar projects")
        .ok();
    assertThat(packageExplorerBot.hasItems(), is(false));

    // Enable Sonar nature
    ProjectUtils.configureProject(PROJECT_NAME);

    assertThat(packageExplorerBot.hasItems(), is(true));
  }
}
