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
package org.sonar.ide.eclipse.core.internal.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

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
import org.sonar.ide.eclipse.common.servers.ISonarServer;
import org.sonar.ide.eclipse.core.configurator.SonarConfiguratorProperties;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.SonarProperties;
import org.sonar.ide.eclipse.core.internal.markers.MarkerUtils;
import org.sonar.ide.eclipse.core.internal.resources.SonarProperty;
import org.sonar.ide.eclipse.core.internal.servers.ISonarServersManager;
import org.sonar.ide.eclipse.tests.common.SonarTestCase;

public class AnalyzeProjectJobTest extends SonarTestCase {

  public org.junit.rules.ExternalResource test = null;
  private static IProject project;
  private static ISonarServersManager serversManager;

  @BeforeClass
  public static void prepare() throws Exception {
    serversManager = SonarCorePlugin.getServersManager();
    final ISonarServer server = serversManager.create("http://localhost:9000", null, null);
    SonarCorePlugin.getServersManager().addServer(new FakedVersionedSonarServer(server, "4.0"));

    project = importEclipseProject("reference");

    // Enable Sonar Nature
    SonarCorePlugin.createSonarProject(project, "http://localhost:9000", "bar:foo");
  }

  @Before
  public void cleanup() {
    MarkerUtils.deleteIssuesMarkers(project);
  }

  private static AnalyzeProjectJob job(final IProject project) {
    return new AnalyzeProjectJob(new AnalyseProjectRequest(project));
  }

  @Test
  public void shouldConfigureAnalysis() throws Exception {
    final AnalyzeProjectJob job = job(project);
    job.setIncremental(true);
    final Properties props = new Properties();
    job.configureAnalysis(MONITOR, props, new ArrayList<SonarProperty>());

    assertThat(props.get(SonarProperties.SONAR_URL).toString()).isEqualTo("http://localhost:9000");
    assertThat(props.get(SonarProperties.PROJECT_KEY_PROPERTY).toString()).isEqualTo("bar:foo");
    assertThat(props.get(SonarProperties.ANALYSIS_MODE).toString()).isEqualTo("incremental");
    // SONARIDE-386 check that at least some JARs from the VM are appended
    final List<String> libs = Arrays.asList(props.get(SonarConfiguratorProperties.LIBRARIES_PROPERTY).toString().split(","));
    assertThat(libs).doesNotHaveDuplicates();
    boolean foundRT = false;
    for (final String lib : libs) {
      if (lib.endsWith("rt.jar") || lib.endsWith("classes.jar") /* For Mac JDK 1.6 */) {
        foundRT = true;
        break;
      }
    }
    if (!foundRT) {
      fail("rt.jar/classes.jar not found in sonar.libraries: " + props.get(SonarConfiguratorProperties.LIBRARIES_PROPERTY).toString());
    }
  }

  @Test
  public void shouldForceFullPreview() throws Exception {
    final AnalyzeProjectJob job = job(project);
    job.setIncremental(false);
    final Properties props = new Properties();
    job.configureAnalysis(MONITOR, props, new ArrayList<SonarProperty>());

    assertThat(props.get(SonarProperties.SONAR_URL).toString()).isEqualTo("http://localhost:9000");
    assertThat(props.get(SonarProperties.PROJECT_KEY_PROPERTY).toString()).isEqualTo("bar:foo");
    assertThat(props.get(SonarProperties.ANALYSIS_MODE).toString()).isEqualTo("preview");
  }

  @Test
  public void shouldConfigureAnalysisWithExtraProps() throws Exception {
    final AnalyzeProjectJob job = job(project);
    final Properties props = new Properties();
    job.configureAnalysis(MONITOR, props, Arrays.asList(new SonarProperty("sonar.foo", "value")));

    assertThat(props.get("sonar.foo").toString()).isEqualTo("value");
  }

  @Test
  public void userConfiguratorShouldOverrideConfiguratorHelperProps() throws Exception {
    final AnalyzeProjectJob job = job(project);
    Properties props = new Properties();
    job.configureAnalysis(MONITOR, props, Arrays.<SonarProperty>asList());

    assertThat(props.get("sonar.java.source").toString()).isNotEqualTo("fake");

    props = new Properties();
    job.configureAnalysis(MONITOR, props, Arrays.asList(new SonarProperty("sonar.java.source", "fake")));

    assertThat(props.get("sonar.java.source").toString()).isEqualTo("fake");
  }

  @Test
  public void shouldCreateMarkersFromIssuesReport() throws Exception {
    final AnalyzeProjectJob job = job(project);
    job.createMarkersFromReportOutput(new File("testdata/sonar-report.json"));

    final List<IMarker> markers = Arrays.asList(project.findMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE));
    assertThat(markers.size()).isEqualTo(6);

    Assert.assertThat(markers, JUnitMatchers.hasItem(new IsMarker("src/Findbugs.java", 5)));
    Assert.assertThat(markers, JUnitMatchers.hasItem(new IsMarker("src/Pmd.java", 2)));
    Assert.assertThat(markers, JUnitMatchers.hasItem(new IsMarker("src/Checkstyle.java", 1)));
  }

  @Test
  public void shouldCleanAndCreateMarkersFromIncrementalAnalysis() throws Exception {
    AnalyzeProjectJob job = job(project);
    job.createMarkersFromReportOutput(new File("testdata/sonar-report.json"));

    // During incremental analysis PMD file was not modified, Findbugs has one remaing issue and Checkstyle has no remaining issues
    job = job(project);
    job.createMarkersFromReportOutput(new File("testdata/sonar-report-incremental.json"));

    final List<IMarker> markers = Arrays.asList(project.findMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE));
    assertThat(markers.size()).isEqualTo(3);

    Assert.assertThat(markers, JUnitMatchers.hasItem(new IsMarker("src/Findbugs.java", 5)));
    Assert.assertThat(markers, JUnitMatchers.hasItem(new IsMarker("src/Pmd.java", 2)));
  }

  static class IsMarker extends BaseMatcher<IMarker> {

    private final String path;
    private final int line;

    public IsMarker(final String path, final int line) {
      this.path = path;
      this.line = line;
    }

    @Override
    public boolean matches(final Object item) {
      final IMarker marker = (IMarker) item;
      final String actualPath = marker.getResource().getProjectRelativePath().toString();
      final int actualLine = marker.getAttribute(IMarker.LINE_NUMBER, -1);
      return StringUtils.equals(actualPath, path) && actualLine == line;
    }

    @Override
    public void describeTo(final Description description) {
      // TODO Auto-generated method stub
    }

  }

}
