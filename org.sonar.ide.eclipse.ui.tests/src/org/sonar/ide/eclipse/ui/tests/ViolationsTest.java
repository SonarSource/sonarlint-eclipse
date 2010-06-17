package org.sonar.ide.eclipse.ui.tests;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.Before;
import org.junit.Test;
import org.sonar.ide.eclipse.SonarPlugin;

public class ViolationsTest extends UITestCase {

  @Before
  public void setUp() throws Exception {
    configureDefaultSonarServer();
  }

  @Test
  public void testRefreshViolations() throws Exception {
    final String projectName = "SimpleProject";
    importAndConfigureNonMavenProject(projectName);

    final SWTBotTree tree = selectProject(projectName);

    assertThat(getMarkers(projectName, SonarPlugin.MARKER_VIOLATION_ID).length, is(0));

    ContextMenuHelper.clickContextMenu(tree, "Sonar", "Refresh violations");
    waitForAllBuildsToComplete();
    bot.sleep(1000 * 30); // TODO Godin: looks like waitForAllBuildsToComplete(); doesn't work

    assertThat(getMarkers(projectName, SonarPlugin.MARKER_VIOLATION_ID).length, greaterThan(0));
  }

  private IMarker[] getMarkers(final String projectName, final String markerId) throws Exception {
    final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    return project.findMarkers(markerId, true, IResource.DEPTH_INFINITE);
  }

}
