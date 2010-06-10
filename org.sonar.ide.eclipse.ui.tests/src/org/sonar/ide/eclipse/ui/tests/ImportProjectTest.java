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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.junit.After;
import org.junit.Test;

/**
 * @author Evgeny Mandrikov
 */
public class ImportProjectTest extends UITestCase {
  @After
  public void tearDown() throws Exception {
    clearProjects();
  }
  
  @Test
  public void testNonMavenProject() throws Exception {
    String projectName = "SimpleProject";
    importNonMavenProject(projectName);
    
    check("", projectName);
  }

  @Test
  public void testSimpleModuleMavenProject() throws Exception {
    String projectName = "hello-world";
    String groupId = "org.sonar-ide.tests." + projectName;
    importMavenProject(projectName);

    check(groupId, "hello-world");
  }

  @Test
  public void testMultiModuleMavenProject() throws Exception {
    String projectName = "modules";
    String groupId = "org.sonar-ide.tests." + projectName;
    importMavenProject(projectName);
    
    check(groupId, "parent");
    check(groupId, "module_a");
    check(groupId, "submodule_a1");
    check(groupId, "submodule_a2");
    check(groupId, "module_b");
    check(groupId, "submodule_b1");
    check(groupId, "submodule_b2");
  }
  
  private void check(String groupId, String artifactId) {
    SWTBotShell shell = showSonarPropertiesPage(artifactId);
    assertProperties(shell, groupId, artifactId);
    closeProperties(shell);    
  }

  private void assertProperties(SWTBotShell shell, String expectedGroupId, String expectedArtifactId) {
    String groupId = shell.bot().textWithLabel("GroupId :").getText();
    String artifactId = shell.bot().textWithLabel("ArtifactId :").getText();
    String branch = shell.bot().textWithLabel("Branch :").getText();
    assertThat(groupId, is(expectedGroupId));
    assertThat(artifactId, is(expectedArtifactId));
    assertThat(branch, is(""));
  }

  private void closeProperties(SWTBotShell shell) {
    try {
      bot.button("Cancel").click();
    } finally {
      waitForClose(shell);
    }
  }
}
