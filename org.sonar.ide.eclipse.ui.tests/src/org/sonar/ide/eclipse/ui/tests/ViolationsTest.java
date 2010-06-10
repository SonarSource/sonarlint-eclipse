package org.sonar.ide.eclipse.ui.tests;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.Test;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.test.SonarTestServer;

public class ViolationsTest extends UITestCase {

  @Test
  public void testRefreshViolations() throws Exception {
    SonarTestServer server = getTestServer();
    configureDefaultSonarServer(server.getBaseUrl());

    String projectName = "SimpleProject";
    importNonMavenProject(projectName);

    SWTBotShell shell = showSonarPropertiesPage(projectName);
    shell.bot().textWithLabel("GroupId :").setText(getGroupId(projectName));

    shell.bot().button("Apply").click();
    shell.bot().button("Cancel").click();
    bot.waitUntil(Conditions.shellCloses(shell));

    SWTBotTree tree = selectProject(projectName);
    ContextMenuHelper.clickContextMenu(tree, "Sonar", "Refresh violations");
    waitForAllBuildsToComplete();
    bot.sleep(1000 * 10); // TODO Godin: looks like waitForAllBuildsToComplete(); doesn't work

    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    final IMarker[] markers = project.findMarkers(SonarPlugin.MARKER_ID, true, IResource.DEPTH_INFINITE);
    assertThat(markers.length, is(4));

    server.stop();
  }

}
