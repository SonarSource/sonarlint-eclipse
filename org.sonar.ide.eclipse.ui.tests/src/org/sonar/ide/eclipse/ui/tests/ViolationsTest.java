package org.sonar.ide.eclipse.ui.tests;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.Test;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.test.SonarTestServer;

public class ViolationsTest extends UITestCase {

  @Test
  public void testRefreshViolations() throws Exception {
    SonarTestServer server = getTestServer();
    configure(server.getBaseUrl());

    String projectName = "SimpleProject";
    importNonMavenProject(projectName);

    SWTBotShell shell = showSonarPropertiesPage(projectName);
    shell.bot().textWithLabel("GroupId :").setText("org.sonar-ide.tests." + projectName);
    try {
      shell.bot().button("Apply").click();
      shell.bot().button("Cancel").click();
    } finally {
      waitForClose(shell);
    }

    SWTBotTree tree = selectProject(projectName);
    ContextMenuHelper.clickContextMenu(tree, "Sonar", "Refresh violations");
    waitForAllBuildsToComplete();
    bot.sleep(1000 * 10); // TODO Godin: looks like waitForAllBuildsToComplete(); doesn't work 

    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    final IMarker[] markers = project.findMarkers(SonarPlugin.MARKER_ID, true, IResource.DEPTH_INFINITE);
    assertThat(markers.length, is(4));

    server.stop();
  }

  private void configure(String serverUrl) {
    bot.menu("Window").menu("Preferences").click();
    SWTBotShell shell = bot.shell("Preferences");
    shell.activate();
    bot.tree().select("Sonar");
    shell.bot().button("Edit").click();

    SWTBotShell shell2 = bot.shell(""); // TODO Godin: should be unique name
    shell2.bot().textWithLabel("Sonar server URL :").setText(serverUrl);
    try {
      shell2.bot().button("Finish").click();
    } finally {
      waitForClose(shell2);
    }

    try {
      shell.bot().button("OK").click();
    } finally {
      waitForClose(shell);
    }
  }

}
