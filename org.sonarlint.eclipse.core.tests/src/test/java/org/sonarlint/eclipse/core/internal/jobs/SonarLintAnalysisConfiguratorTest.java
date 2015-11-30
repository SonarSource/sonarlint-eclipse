/*
 * SonarLint for Eclipse
 * Copyright (C) 2015 SonarSource
 * sonarlint@sonarsource.com
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
package org.sonarlint.eclipse.core.internal.jobs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.eclipse.core.resources.IProject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonarlint.eclipse.core.internal.SonarLintNature;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProperty;
import org.sonarlint.eclipse.tests.common.SonarTestCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class SonarLintAnalysisConfiguratorTest extends SonarTestCase {

  public org.junit.rules.ExternalResource test = null;
  private static IProject project;

  @BeforeClass
  public static void prepare() throws Exception {

    project = importEclipseProject("reference");

    // Enable Sonar Nature
    SonarLintNature.enableNature(project);
  }

  @Test
  public void shouldConfigureAnalysis() throws Exception {
    Properties props = new Properties();
    SonarLintAnalysisConfigurator.configureAnalysis(new AnalyzeProjectRequest(project, null), props, new ArrayList<SonarLintProperty>(), MONITOR);

    assertThat(props).doesNotContainKey(SonarLintProperties.PROJECT_KEY_PROPERTY);
    // SONARIDE-386 check that at least some JARs from the VM are appended
    List<String> libs = Arrays.asList(props.get("sonar.libraries").toString().split(","));
    assertThat(libs).doesNotHaveDuplicates();
    boolean foundRT = false;
    for (String lib : libs) {
      if (lib.endsWith("rt.jar") || lib.endsWith("classes.jar") /* For Mac JDK 1.6 */) {
        foundRT = true;
        break;
      }
    }
    if (!foundRT) {
      fail("rt.jar/classes.jar not found in sonar.libraries: " + props.get("sonar.libraries").toString());
    }
  }

  @Test
  public void shouldConfigureAnalysisWithExtraProps() throws Exception {
    Properties props = new Properties();
    SonarLintAnalysisConfigurator.configureAnalysis(new AnalyzeProjectRequest(project, null), props, Arrays.asList(new SonarLintProperty("sonar.foo", "value")), MONITOR);

    assertThat(props.get("sonar.foo").toString()).isEqualTo("value");
  }

  @Test
  public void userConfiguratorShouldOverrideConfiguratorHelperProps() throws Exception {
    Properties props = new Properties();
    SonarLintAnalysisConfigurator.configureAnalysis(new AnalyzeProjectRequest(project, null), props, new ArrayList<SonarLintProperty>(), MONITOR);

    assertThat(props.get("sonar.java.source").toString()).isNotEqualTo("fake");

    props = new Properties();
    SonarLintAnalysisConfigurator.configureAnalysis(new AnalyzeProjectRequest(project, null), props, Arrays.asList(new SonarLintProperty("sonar.java.source", "fake")), MONITOR);

    assertThat(props.get("sonar.java.source").toString()).isEqualTo("fake");
  }

}
