/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonar.ide.eclipse.ui.its;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.ide.eclipse.ui.its.bots.ImportProjectBot;
import org.sonar.ide.eclipse.ui.its.bots.JavaPackageExplorerBot;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class OpenInBrowserActionTest extends AbstractSQEclipseUITest {
  private static final String PROJECT_NAME = "reference";

  public static final String WEB_VIEW_ID = "org.sonar.ide.eclipse.ui.views.ResourceWebView";

  private static final String[] MENUBAR_PATH = {"SonarQube", "Open in SonarQube Server"};

  @BeforeClass
  public static void importProject() throws Exception {
    new ImportProjectBot(bot).setPath(getProjectPath(PROJECT_NAME)).finish();

    // Enable Sonar nature
    configureProject(PROJECT_NAME);
  }

  public static SWTBotView getWebView() {
    try {
      return bot.viewById(WEB_VIEW_ID);
    } catch (WidgetNotFoundException e) {
      return null;
    }
  }

  @Before
  public void closeWebView() {
    if (getWebView() != null) {
      getWebView().close();
    }
  }

  @Test
  public void canOpenFile() {
    new JavaPackageExplorerBot(bot)
      .expandAndSelect(PROJECT_NAME, "src/main/java", "(default package)", "ClassOnDefaultPackage.java")
      .clickContextMenu(MENUBAR_PATH);
    assertThat(getWebView(), is(notNullValue()));
  }

  @Test
  public void canOpenPackage() {
    new JavaPackageExplorerBot(bot)
      .expandAndSelect(PROJECT_NAME, "src/main/java", "(default package)")
      .clickContextMenu(MENUBAR_PATH);
    assertThat(getWebView(), is(notNullValue()));
  }

  @Test
  public void canOpenProject() {
    new JavaPackageExplorerBot(bot)
      .expandAndSelect(PROJECT_NAME)
      .clickContextMenu(MENUBAR_PATH);
    assertThat(getWebView(), is(notNullValue()));
  }
}
