/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.ui.tests;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.utils.SWTUtils;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.SonarServerManager;
import org.sonar.ide.eclipse.tests.common.JobHelpers;
import org.sonar.ide.eclipse.tests.common.VersionHelpers;
import org.sonar.ide.eclipse.tests.common.WorkspaceHelpers;
import org.sonar.ide.eclipse.ui.tests.utils.SwtBotUtils;
import org.sonar.ide.test.SonarIdeTestCase;
import org.sonar.ide.test.SonarTestServer;

import java.io.File;
import java.io.IOException;

/**
 * TODO use Xvfb ("fake" X-server)
 * 
 * @author Evgeny Mandrikov
 */
public abstract class UITestCase extends SonarIdeTestCase {

  private static final String SCREENSHOTS_DIR = "target/screenshots";

  protected static SWTWorkbenchBot bot;

  @BeforeClass
  public final static void beforeClass() throws Exception {
    // Disable Secure Storage during tests
    SonarServerManager.enableSecureStorate(false);
    // Remove all configured server
    SonarPlugin.getServerManager().clean();

    System.out.println("Eclipse version : " + VersionHelpers.getEclipseVersion());

    SWTBotPreferences.SCREENSHOTS_DIR = SCREENSHOTS_DIR;
    SWTBotPreferences.SCREENSHOT_FORMAT = "png";
    bot = new SWTWorkbenchBot();

    SwtBotUtils.closeViewQuietly(bot, "org.eclipse.ui.internal.introview");
    SwtBotUtils.closeViewQuietly(bot, "org.eclipse.ui.views.ContentOutline");
    SwtBotUtils.closeViewQuietly(bot, "org.eclipse.mylyn.tasks.ui.views.tasks");

    // Clean out projects left over from previous test runs
    WorkspaceHelpers.cleanWorkspace();

    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);
  }

  @AfterClass
  public final static void afterClass() throws Exception {
    WorkspaceHelpers.cleanWorkspace();
    bot.sleep(2000);
    bot.resetWorkbench();
  }

  @After
  public final void finalShot() throws IOException {
    takeScreenShot(getClass().getSimpleName());
  }

  /**
   * Don't use this method for UI testing, because in future we'd like to use real Sonar server. Use {@link #getSonarServerUrl()} instead of
   * it.
   */
  @Override
  protected SonarTestServer getTestServer() throws Exception {
    return super.getTestServer();
  }

  public String getSonarServerUrl() throws Exception {
    // TODO Godin: should be possible to use real Sonar server
    return getTestServer().getBaseUrl();
  }

  public static File takeScreenShot(String classifier) throws IOException {
    File parent = new File(SCREENSHOTS_DIR);
    parent.mkdirs();
    File output = File.createTempFile("swtbot", "-" + classifier + ".png", parent);
    SWTUtils.captureScreenshot(output.getAbsolutePath());
    return output;
  }

  public static Exception takeScreenShot(Throwable e) throws IOException {
    File output = takeScreenShot("exception");
    return new Exception(e.getMessage() + " - " + output, e);
  }

  /**
   * Imports maven project. <br/>
   * Don't use this method a lot, because we shouldn't depend closely on m2eclipse. <br/>
   * TODO Move it out of here into m2eclipse module.
   */
  protected File importMavenProject(String projectName) throws Exception {
    File project = getProject(projectName);
    waitForAllBuildsToComplete();
    bot.menu("File").menu("Import...").click();
    SWTBotShell shell = bot.shell("Import");
    try {
      shell.activate();
      bot.tree().expandNode("Maven").select("Existing Maven Projects");
      bot.button("Next >").click();
      bot.comboBoxWithLabel("Root Directory:").setText(project.getCanonicalPath());
      bot.button("Refresh").click();
      bot.button("Finish").click();
    } finally {
      SwtBotUtils.waitForClose(shell);
    }
    waitForAllBuildsToComplete();
    return project;
  }

  protected void waitForAllBuildsToComplete() {
    JobHelpers.waitForJobsToComplete();
  }

  protected static String getGroupId(String projectName) {
    return "org.sonar-ide.tests." + projectName;
  }

}
