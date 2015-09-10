/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonar.ide.eclipse.ui.its;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.junit.Test;
import org.sonar.ide.eclipse.ui.its.bots.ConfigureProjectsWizardBot;
import org.sonar.ide.eclipse.ui.its.bots.ImportProjectBot;
import org.sonar.ide.eclipse.ui.its.bots.IssuesViewBot;
import org.sonar.ide.eclipse.ui.its.bots.JavaPackageExplorerBot;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class IssuesViewTest extends AbstractSQEclipseUITest {
  private static final String PROJECT_NAME = "reference";

  public static final String ISSUES_VIEW_ID = "org.sonar.ide.eclipse.ui.views.issues.IssuesView";

  @Test
  public void shouldShowIssuesForSelectionAndChildren() throws Exception {
    new IssuesViewBot(bot);

    SWTBotView view = bot.viewById(ISSUES_VIEW_ID);
    view.show();

    assertThat(view.bot().tree().hasItems(), is(false));

    new ImportProjectBot(bot).setPath(getProjectPath(PROJECT_NAME)).finish();

    // Enable Sonar nature
    new JavaPackageExplorerBot(bot)
      .expandAndSelect(PROJECT_NAME)
      .clickContextMenu("Configure", "Associate with SonarQube Server...");

    ConfigureProjectsWizardBot projectWizardBot = new ConfigureProjectsWizardBot(bot);
    projectWizardBot.finish();

    new JavaPackageExplorerBot(bot)
      .expandAndSelect(PROJECT_NAME, "src/main/java", "(default package)", "ClassOnDefaultPackage.java");

    // By default issues are grouped by severity
    assertThat(view.bot().tree().getAllItems().length, is(2));
    assertThat(view.bot().tree().expandNode("Critical (1 item)").getItems().length, is(1));
    assertThat(view.bot().tree().expandNode("Major (2 items)").getItems().length, is(2));

    // Group By is a dynamic menu not supported by SWTBot
    // view.bot().menu("Group By").menu("None").click();
    // bot.sleep(1000);
    // assertThat(view.bot().tree().getAllItems().length, is(5));

    new JavaPackageExplorerBot(bot)
      .expandAndSelect(PROJECT_NAME, "src/main/java", "com.foo", "ClassWithFalsePositive.java");

    assertThat(view.bot().tree().getAllItems().length, is(0));
  }

}
