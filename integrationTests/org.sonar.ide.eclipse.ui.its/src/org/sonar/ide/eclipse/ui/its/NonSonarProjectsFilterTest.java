/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonar.ide.eclipse.ui.its;

import org.junit.Test;
import org.sonar.ide.eclipse.ui.its.bots.ImportProjectBot;
import org.sonar.ide.eclipse.ui.its.bots.JavaPackageExplorerBot;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class NonSonarProjectsFilterTest extends AbstractSQEclipseUITest {
  private static final String PROJECT_NAME = "reference";

  @Test
  public void test() throws Exception {
    new ImportProjectBot(bot).setPath(getProjectPath(PROJECT_NAME)).finish();

    JavaPackageExplorerBot packageExplorerBot = new JavaPackageExplorerBot(bot);

    assertThat(packageExplorerBot.hasItems(), is(true));

    // Enable "Non-Sonar projects" filter
    packageExplorerBot.filters().check("Non-SonarQube projects").ok();
    assertThat(packageExplorerBot.hasItems(), is(false));

    // Enable Sonar nature
    configureProject(PROJECT_NAME);

    assertThat(packageExplorerBot.hasItems(), is(true));

    // Disable "Non-Sonar projects" filter
    packageExplorerBot.filters().uncheck("Non-SonarQube projects").ok();
    assertThat(packageExplorerBot.hasItems(), is(true));
  }
}
