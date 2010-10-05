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

package org.sonar.ide.eclipse.jdt.internal;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.junit.Test;
import org.sonar.ide.eclipse.actions.ToggleNatureAction;
import org.sonar.ide.eclipse.core.ISonarResource;
import org.sonar.ide.eclipse.properties.ProjectProperties;
import org.sonar.ide.eclipse.tests.common.SonarTestCase;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class JavaElementsAdapterFactoryTest extends SonarTestCase {

  @Test
  public void shouldAdaptResourceToSonar() throws Exception {
    IProject project = importEclipseProject("SimpleProject");
    // Configure the project
    ProjectProperties properties = ProjectProperties.getInstance(project);
    properties.setUrl("http://localhost:9000");
    String groupId = "org.sonar-ide.tests.SimpleProject";
    String artifactId = "SimpleProject";
    properties.setGroupId(groupId);
    properties.setArtifactId(artifactId);
    properties.save();
    ToggleNatureAction.enableNature(project);

    JavaElementsAdapterFactory factory = new JavaElementsAdapterFactory();

    {
      ISonarResource sonarElement = (ISonarResource) factory.getAdapter(project, ISonarResource.class);
      assertThat(sonarElement, notNullValue());
      assertThat(sonarElement.getKey(), is(groupId + ":" + artifactId));
    }

    {
      IFolder folder = project.getFolder("src/main/java");
      ISonarResource sonarElement = (ISonarResource) factory.getAdapter(folder, ISonarResource.class);
      assertThat(sonarElement, notNullValue());
      assertThat(sonarElement.getKey(), is(groupId + ":" + artifactId + ":[default]"));
    }

    {
      IFile file = project.getFile("src/main/java/ViolationOnFile.java");
      ISonarResource sonarElement = (ISonarResource) factory.getAdapter(file, ISonarResource.class);
      assertThat(sonarElement, notNullValue());
      assertThat(sonarElement.getKey(), is(groupId + ":" + artifactId + ":[default].ViolationOnFile"));
    }
  }
}
