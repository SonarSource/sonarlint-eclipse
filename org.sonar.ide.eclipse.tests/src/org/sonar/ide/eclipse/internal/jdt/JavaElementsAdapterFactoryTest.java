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

package org.sonar.ide.eclipse.internal.jdt;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.ide.eclipse.core.ISonarFile;
import org.sonar.ide.eclipse.core.ISonarProject;
import org.sonar.ide.eclipse.core.ISonarResource;
import org.sonar.ide.eclipse.internal.ui.actions.ToggleNatureAction;
import org.sonar.ide.eclipse.internal.ui.properties.ProjectProperties;
import org.sonar.ide.eclipse.tests.common.SonarTestCase;

public class JavaElementsAdapterFactoryTest extends SonarTestCase {

  private static final String groupId = "org.sonar-ide.tests.SimpleProject";
  private static final String artifactId = "SimpleProject";
  private static IProject project;

  /**
   * Workaround for accessing non static method {@link #importEclipseProject(String)} from static method {@link #importProject()}
   */
  static class TTT extends SonarTestCase {
  }

  @BeforeClass
  public static void importProject() throws Exception {
    project = new TTT().importEclipseProject("SimpleProject");
    // Configure the project
    ProjectProperties properties = ProjectProperties.getInstance(project);
    properties.setUrl("http://localhost:9000");
    properties.setGroupId(groupId);
    properties.setArtifactId(artifactId);
    properties.save();
    ToggleNatureAction.enableNature(project);
  }

  private JavaElementsAdapterFactory factory;

  @Before
  public void setUp() {
    factory = new JavaElementsAdapterFactory();
  }

  @Test
  public void testAdapterList() {
    assertThat(factory.getAdapterList().length, is(5));
  }

  @Test
  public void shouldAdaptProjectToSonarProject() {
    ISonarProject sonarElement = (ISonarProject) factory.getAdapter(project, ISonarProject.class);
    assertThat(sonarElement, notNullValue());
    assertThat(sonarElement.getKey(), is(groupId + ":" + artifactId));
  }

  @Test
  public void shouldAdaptFolderToSonarResource() {
    IFolder folder = project.getFolder("src/main/java");
    ISonarResource sonarElement = (ISonarResource) factory.getAdapter(folder, ISonarResource.class);
    assertThat(sonarElement, notNullValue());
    assertThat(sonarElement.getKey(), is(groupId + ":" + artifactId + ":[default]"));
  }

  @Test
  public void shouldAdaptFileToSonarFile() {
    IFile file = project.getFile("src/main/java/ViolationOnFile.java");
    ISonarFile sonarElement = (ISonarFile) factory.getAdapter(file, ISonarFile.class);
    assertThat(sonarElement, notNullValue());
    assertThat(sonarElement.getKey(), is(groupId + ":" + artifactId + ":[default].ViolationOnFile"));
  }
}
