/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.ide.eclipse.core.internal.resources;

import org.eclipse.core.resources.IProject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.tests.common.SonarTestCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

public class SonarProjectManagerTest extends SonarTestCase {

  private static IProject project;

  SonarProjectManager sonarProjectManager = new SonarProjectManager();

  @BeforeClass
  public static void prepare() throws Exception {
    project = importEclipseProject("reference");

    // Enable Sonar Nature
    SonarCorePlugin.createSonarProject(project, "http://localhost:9000", "bar:foo");
  }

  @Test
  public void shouldSaveAndRead() throws Exception {
    SonarProject sonarConfiguration = sonarProjectManager.readSonarConfiguration(project);
    new SonarProjectManager().saveSonarConfiguration(project, sonarConfiguration);
  }

  @Test
  public void shouldSaveAndReadWithEmptyPropertyValue() throws Exception {
    SonarProject sonarConfiguration = sonarProjectManager.readSonarConfiguration(project);
    sonarConfiguration.getExtraProperties().add(new SonarProperty("sonar.foo", ""));
    sonarProjectManager.saveSonarConfiguration(project, sonarConfiguration);
    // Reload
    sonarConfiguration = sonarProjectManager.readSonarConfiguration(project);
    assertThat(sonarConfiguration.getExtraProperties()).extracting("name", "value").contains(tuple("sonar.foo", ""));

  }

}
