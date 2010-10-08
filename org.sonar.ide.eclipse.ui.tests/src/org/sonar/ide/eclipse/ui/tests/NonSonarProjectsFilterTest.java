package org.sonar.ide.eclipse.ui.tests;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;
import org.sonar.ide.eclipse.ui.tests.bots.ImportProjectBot;
import org.sonar.ide.eclipse.ui.tests.bots.PackageExplorerBot;
import org.sonar.ide.eclipse.ui.tests.utils.ProjectUtils;

public class NonSonarProjectsFilterTest extends UITestCase {

  @Test
  public void test() throws Exception {
    new ImportProjectBot().setPath(getProject("SimpleProject").getCanonicalPath()).finish();

    PackageExplorerBot packageExplorerBot = new PackageExplorerBot();

    assertThat(packageExplorerBot.hasItems(), is(true));

    // Enable "Non-Sonar projects" filter
    packageExplorerBot
        .filters()
        .check("Non-Sonar projects")
        .ok();
    assertThat(packageExplorerBot.hasItems(), is(false));

    // Enable Sonar nature
    ProjectUtils.configureProject("SimpleProject", "http://localhost:9000");

    assertThat(packageExplorerBot.hasItems(), is(true));
  }
}
