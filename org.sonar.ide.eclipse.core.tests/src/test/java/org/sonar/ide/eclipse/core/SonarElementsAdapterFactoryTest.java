/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.core;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.ide.eclipse.core.SonarElementsAdapterFactory;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.resources.ISonarFile;
import org.sonar.ide.eclipse.core.resources.ISonarResource;
import org.sonar.ide.eclipse.tests.common.SonarTestCase;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class SonarElementsAdapterFactoryTest extends SonarTestCase {

  private static final String key = "org.sonar-ide.tests.SimpleProject:SimpleProject";
  private static IProject project;

  @BeforeClass
  public static void importProject() throws Exception {
    project = importEclipseProject("SimpleProject");
    // Configure the project
    SonarCorePlugin.createSonarProject(project, "http://localhost:9000", key);
  }

  private SonarElementsAdapterFactory factory;

  @Before
  public void setUp() {
    factory = new SonarElementsAdapterFactory();
  }

  @Test
  public void testAdapterList() {
    assertThat(factory.getAdapterList().length, is(2));
  }

  @Test
  @Ignore("Need a connection to server to know server version")
  public void shouldAdaptFolderToSonarResource() {
    IFolder folder = project.getFolder("src/main/java");
    ISonarResource sonarElement = (ISonarResource) factory.getAdapter(folder, ISonarResource.class);
    assertThat(sonarElement, notNullValue());
    assertThat(sonarElement.getKey(), is(key + ":[default]"));
  }

  @Test
  @Ignore("Need a connection to server to know server version")
  public void shouldAdaptFileToSonarFile() {
    IFile file = project.getFile("src/main/java/ViolationOnFile.java");
    ISonarFile sonarElement = (ISonarFile) factory.getAdapter(file, ISonarFile.class);
    assertThat(sonarElement, notNullValue());
    assertThat(sonarElement.getKey(), is(key + ":[default].ViolationOnFile"));
  }
}
