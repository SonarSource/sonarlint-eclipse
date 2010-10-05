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

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.ide.eclipse.core.ISonarConstants;

@Ignore("Godin: doesn't work in SonarSource Hudson")
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

    assertThat(getMarkers(projectName, ISonarConstants.MARKER_ID).length, is(0));

    ContextMenuHelper.clickContextMenu(tree, "Sonar", "Refresh violations");
    waitForAllBuildsToComplete();
    bot.sleep(1000 * 30); // TODO Godin: looks like waitForAllBuildsToComplete(); doesn't work

    assertThat(getMarkers(projectName, ISonarConstants.MARKER_ID).length, greaterThan(0));
  }

  private IMarker[] getMarkers(final String projectName, final String markerId) throws Exception {
    final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    return project.findMarkers(markerId, true, IResource.DEPTH_INFINITE);
  }

}
