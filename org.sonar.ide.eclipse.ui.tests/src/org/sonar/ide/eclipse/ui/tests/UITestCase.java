/*
 * Copyright (C) 2010 Evgeny Mandrikov
 *
 * Sonar-IDE is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar-IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar-IDE; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.ui.tests;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.utils.SWTUtils;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.sonar.ide.eclipse.tests.common.JobHelpers;
import org.sonar.ide.eclipse.tests.common.VersionHelpers;
import org.sonar.ide.eclipse.tests.common.WorkspaceHelpers;
import org.sonar.ide.test.SonarIdeTestCase;
import org.sonar.ide.test.SonarTestServer;

/**
 * TODO use Xvfb ("fake" X-server)
 * 
 * @author Evgeny Mandrikov
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public abstract class UITestCase extends SonarIdeTestCase {

  private static final String SCREENSHOTS_DIR = "target/screenshots";

  protected static SWTWorkbenchBot bot;

  @BeforeClass
  public final static void beforeClass() throws Exception {
    System.out.println("Eclipse version : " + VersionHelpers.getEclipseVersion());

    SWTBotPreferences.SCREENSHOTS_DIR = SCREENSHOTS_DIR;
    SWTBotPreferences.SCREENSHOT_FORMAT = "png";
    bot = new SWTWorkbenchBot();

    try {
      closeView("org.eclipse.ui.internal.introview");
    } catch (WidgetNotFoundException e) {
      // ignore
    }
    try {
      closeView("org.eclipse.ui.views.ContentOutline");
    } catch (WidgetNotFoundException e) {
      // ignore
    }

    // Clean out projects left over from previous test runs
    clearProjects();

    openPerspective(JavaUI.ID_PERSPECTIVE);
  }

  @AfterClass
  public final static void afterClass() throws Exception {
    clearProjects();
    bot.sleep(2000);
    // bot.resetWorkbench();
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

  protected String getSonarServerUrl() throws Exception {
    // TODO Godin: should be possible to use real Sonar server
    return getTestServer().getBaseUrl();
  }

  protected static void openPerspective(final String id) {
    bot.perspectiveById(id).activate();
  }

  /**
   * @throws WidgetNotFoundException
   *           if view not found
   */
  protected static void closeView(final String id) {
    bot.viewById(id).close();
  }

  protected IEditorPart openFile(IProject project, String relPath) throws PartInitException {
    IFile file = project.getFile(relPath);
    // TODO next line should be executed in UI Thread
    return IDE.openEditor(getActivePage(), file, true);
  }

  protected static IWorkbenchPage getActivePage() {
    IWorkbench workbench = PlatformUI.getWorkbench();
    return workbench.getWorkbenchWindows()[0].getActivePage();
  }

  /**
   * Cleans workspace.
   */
  public static void clearProjects() throws Exception {
    WorkspaceHelpers.cleanWorkspace();
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
   * Imports non-maven project.
   */
  protected File importNonMavenProject(String projectName) throws Exception {
    File project = getProject(projectName);
    waitForAllBuildsToComplete();
    bot.menu("File").menu("Import...").click();
    SWTBotShell shell = bot.shell("Import");
    try {
      shell.activate();
      bot.tree().expandNode("General").select("Existing Projects into Workspace");
      bot.button("Next >").click();
      bot.text().setText(project.getCanonicalPath());
      bot.button("Refresh").click();
      bot.button("Finish").click();
      waitForAllBuildsToComplete();
    } finally {
      waitForClose(shell);
    }
    return project;
  }

  /**
   * Imports non-maven project and sets proper groupId.
   */
  protected File importAndConfigureNonMavenProject(String projectName) throws Exception {
    File project = importNonMavenProject(projectName);
    // Configure
    final SWTBotShell shell = showSonarPropertiesPage(projectName);
    shell.bot().textWithLabel("GroupId :").setText(getGroupId(projectName));

    shell.bot().button("Apply").click();
    shell.bot().button("Cancel").click();
    bot.waitUntil(Conditions.shellCloses(shell));

    return project;
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
      waitForClose(shell);
    }
    waitForAllBuildsToComplete();
    return project;
  }

  protected void waitForAllBuildsToComplete() {
    waitForAllEditorsToSave();
    JobHelpers.waitForJobsToComplete();
  }

  protected void waitForAllEditorsToSave() {
    // TODO JobHelpers.waitForJobs(EDITOR_JOB_MATCHER, 30 * 1000);
  }

  public static boolean waitForClose(SWTBotShell shell) {
    for (int i = 0; i < 50; i++) {
      if ( !shell.isOpen()) {
        return true;
      }
      bot.sleep(200);
    }
    shell.close();
    return false;
  }

  protected static SWTBotShell showSonarPropertiesPage(String projectName) {
    SWTBotTree tree = selectProject(projectName);
    ContextMenuHelper.clickContextMenu(tree, "Properties");
    SWTBotShell shell = bot.shell("Properties for " + projectName);
    shell.activate();
    bot.tree().select("Sonar");
    return shell;
  }

  protected static SWTBotShell showGlobalSonarPropertiesPage() {
    bot.menu("Window").menu("Preferences").click();
    SWTBotShell shell = bot.shell("Preferences");
    shell.activate();
    bot.tree().select("Sonar");
    return shell;
  }

  protected static SWTBotTree selectProject(String projectName) {
    SWTBotTree tree = bot.viewById(JavaUI.ID_PACKAGES).bot().tree();
    SWTBotTreeItem treeItem = null;
    treeItem = tree.getTreeItem(projectName);
    treeItem.select();
    return tree;
  }

  protected static void configureDefaultSonarServer(String serverUrl) {
    SWTBotShell shell = showGlobalSonarPropertiesPage();

    bot.table().getTableItem(0).select();

    bot.button("Edit...").click();

    bot.waitUntil(Conditions.shellIsActive("Edit Sonar server connection"));
    SWTBotShell shell2 = bot.shell("Edit Sonar server connection");
    shell2.activate();
    bot.textWithLabel("Sonar server URL :").setText(serverUrl);

    // Close wizard
    bot.button("Finish").click();
    bot.waitUntil(Conditions.shellCloses(shell2));

    bot.table().getTableItem(0).check(); // TODO

    // Close properties
    shell.bot().button("OK").click();
    bot.waitUntil(Conditions.shellCloses(shell));
  }

  protected void configureDefaultSonarServer() throws Exception {
    configureDefaultSonarServer(getSonarServerUrl());
  }

  protected static String getGroupId(String projectName) {
    return "org.sonar-ide.tests." + projectName;
  }

}
