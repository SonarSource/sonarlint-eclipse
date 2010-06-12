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
import org.sonar.ide.eclipse.tests.common.WorkspaceHelpers;
import org.sonar.ide.test.SonarIdeTestCase;

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
    init(); // TODO Godin: remove
    
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

    openPerspective("org.eclipse.jdt.ui.JavaPerspective");
  }

  @AfterClass
  public final static void afterClass() throws Exception {
    clearProjects();
    bot.sleep(2000);
//    bot.resetWorkbench();
  }

  private static void openPerspective(final String id) {
    bot.perspectiveById(id).activate();
  }

  protected static void closeView(final String id) {
    // TODO Godin: what if view doesn't exists
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

  @After
  public final void finalShot() throws IOException {
    takeScreenShot(getClass().getSimpleName());
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

  protected void configureDefaultSonarServer(String serverUrl) {
    SWTBotShell shell = showGlobalSonarPropertiesPage();
    bot.button("Edit").click();

    bot.waitUntil(Conditions.shellIsActive("Edit sonar server connection"));
    SWTBotShell shell2 = bot.shell("Edit sonar server connection");
    shell2.activate();
    bot.textWithLabel("Sonar server URL :").setText(serverUrl);

    // Close wizard
    bot.button("Finish").click();
    bot.waitUntil(Conditions.shellCloses(shell2));

    // Close properties
    shell.bot().button("OK").click();
    bot.waitUntil(Conditions.shellCloses(shell));
  }
  
  protected static String getGroupId(String projectName) {
    return "org.sonar-ide.tests." + projectName;
  }
}
