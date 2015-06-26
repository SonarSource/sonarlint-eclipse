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
package org.sonar.ide.eclipse.core.internal.jobs;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.SonarProperties;
import org.sonar.ide.eclipse.core.internal.markers.MarkerUtils;
import org.sonar.ide.eclipse.core.internal.resources.SonarProperty;
import org.sonar.ide.eclipse.core.internal.servers.ISonarServersManager;
import org.sonar.ide.eclipse.core.internal.servers.SonarServer;
import org.sonar.ide.eclipse.tests.common.SonarTestCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class AnalyzeProjectJobTest extends SonarTestCase {

  public org.junit.rules.ExternalResource test = null;
  private static IProject project;
  private static SonarServer server;
  private static ISonarServersManager serversManager;

  @BeforeClass
  public static void prepare() throws Exception {
    serversManager = SonarCorePlugin.getServersManager();
    server = (SonarServer) serversManager.create("localhost", "http://localhost:9000", null, null);
    SonarCorePlugin.getServersManager().addServer(server);

    project = importEclipseProject("reference");

    // Enable Sonar Nature
    SonarCorePlugin.createSonarProject(project, "http://localhost:9000", "bar:foo");
  }

  @Before
  public void cleanup() {
    MarkerUtils.deleteIssuesMarkers(project);
  }

  private static AnalyzeProjectJob job(IProject project) {
    return new AnalyzeProjectJob(new AnalyzeProjectRequest(project));
  }

  @Test
  public void shouldConfigureAnalysis() throws Exception {
    AnalyzeProjectJob job = job(project);
    Properties props = new Properties();
    job.configureAnalysis(MONITOR, props, new ArrayList<SonarProperty>());

    assertThat(props.get(SonarProperties.PROJECT_KEY_PROPERTY).toString()).isEqualTo("bar:foo");
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
    AnalyzeProjectJob job = job(project);
    Properties props = new Properties();
    job.configureAnalysis(MONITOR, props, Arrays.asList(new SonarProperty("sonar.foo", "value")));

    assertThat(props.get("sonar.foo").toString()).isEqualTo("value");
  }

  @Test
  public void userConfiguratorShouldOverrideConfiguratorHelperProps() throws Exception {
    AnalyzeProjectJob job = job(project);
    Properties props = new Properties();
    job.configureAnalysis(MONITOR, props, Arrays.<SonarProperty>asList());

    assertThat(props.get("sonar.java.source").toString()).isNotEqualTo("fake");

    props = new Properties();
    job.configureAnalysis(MONITOR, props, Arrays.asList(new SonarProperty("sonar.java.source", "fake")));

    assertThat(props.get("sonar.java.source").toString()).isEqualTo("fake");
  }

  @Test
  public void shouldCreateMarkersFromIssuesReport() throws Exception {
    AnalyzeProjectJob job = job(project);
    job.createMarkersFromReportOutput(MONITOR, new File("testdata/sonar-report.json"));

    List<IMarker> markers = Arrays.asList(project.findMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE));
    assertThat(markers.size()).isEqualTo(6);

    Assert.assertThat(markers, JUnitMatchers.hasItem(new IsMarker("src/Findbugs.java", 5)));
    Assert.assertThat(markers, JUnitMatchers.hasItem(new IsMarker("src/Pmd.java", 2)));
    Assert.assertThat(markers, JUnitMatchers.hasItem(new IsMarker("src/Checkstyle.java", 1)));
  }

  @Test
  public void shouldCleanAndCreateMarkersForSingleFile() throws Exception {
    AnalyzeProjectJob job = job(project);
    job.createMarkersFromReportOutput(MONITOR, new File("testdata/sonar-report.json"));
    List<IMarker> markers = Arrays.asList(project.findMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE));
    assertThat(markers.size()).isEqualTo(6);

    // During single file analysis, Findbugs has one remaing issue
    job = job(project);
    job.createMarkersFromReportOutput(MONITOR, new File("testdata/sonar-report-single.json"));

    markers = Arrays.asList(project.findMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE));
    assertThat(markers.size()).isEqualTo(5);

    Assert.assertThat(markers, JUnitMatchers.hasItem(new IsMarker("src/Findbugs.java", 5)));
  }

  static class IsMarker extends BaseMatcher<IMarker> {

    private final String path;
    private final int line;

    public IsMarker(String path, int line) {
      this.path = path;
      this.line = line;
    }

    @Override
    public boolean matches(Object item) {
      IMarker marker = (IMarker) item;
      String actualPath = marker.getResource().getProjectRelativePath().toString();
      int actualLine = marker.getAttribute(IMarker.LINE_NUMBER, -1);
      return StringUtils.equals(actualPath, path) && (actualLine == line);
    }

    @Override
    public void describeTo(Description description) {
      // TODO Auto-generated method stub
    }

  }

}
