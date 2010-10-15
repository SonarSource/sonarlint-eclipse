package org.sonar.ide.eclipse.ui.tests;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.junit.Test;
import org.sonar.ide.eclipse.core.ISonarConstants;
import org.sonar.ide.eclipse.ui.tests.bots.ImportProjectBot;
import org.sonar.ide.eclipse.ui.tests.bots.PackageExplorerBot;
import org.sonar.ide.eclipse.ui.tests.utils.ProjectUtils;
import org.sonar.ide.eclipse.ui.tests.utils.SwtBotUtils;
import org.sonar.ide.eclipse.views.ViolationsView;

public class ViolationsViewTest extends UITestCase {
  private static final String PROJECT_NAME = "reference";

  @Test
  public void shouldShowViolationsForSelectionAndChildren() throws Exception {
    SwtBotUtils.openPerspective(bot, ISonarConstants.PERSPECTIVE_ID);

    SWTBotView view = bot.viewById(ViolationsView.ID);
    view.show();

    assertThat(view.bot().tree().hasItems(), is(false));

    new ImportProjectBot().setPath(getProjectPath(PROJECT_NAME)).finish();
    // Enable Sonar nature
    ProjectUtils.configureProject(PROJECT_NAME);

    new PackageExplorerBot()
        .expandAndSelect(PROJECT_NAME, "src/main/java", "(default package)", "ClassOnDefaultPackage.java");

    bot.sleep(5000);

    assertThat(view.bot().tree().hasItems(), is(true));
  }
}
