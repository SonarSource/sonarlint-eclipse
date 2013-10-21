/*
 * Sonar Eclipse
 * Copyright (C) 2010-2013 SonarSource
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

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.SonarProperties;
import org.sonar.ide.eclipse.core.internal.markers.MarkerUtils;
import org.sonar.ide.eclipse.core.internal.resources.SonarProperty;
import org.sonar.ide.eclipse.tests.common.SonarTestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

public class AnalyseProjectJobTest extends SonarTestCase {

  public org.junit.rules.ExternalResource test = null;
  private static IProject project;

  @BeforeClass
  public static void prepare() throws Exception {
    SonarCorePlugin.getServersManager().addServer("http://localhost:9000", null, null);

    project = importEclipseProject("reference");

    // Enable Sonar Nature
    SonarCorePlugin.createSonarProject(project, "http://localhost:9000", "bar:foo");
  }

  @Before
  public void cleanup() {
    MarkerUtils.deleteIssuesMarkers(project);
  }

  private static AnalyseProjectJob job(IProject project) {
    return new AnalyseProjectJob(new AnalyseProjectRequest(project));
  }

  @Test
  public void shouldConfigureAnalysisBefore40() throws Exception {
    AnalyseProjectJob job = job(project);
    job.setIncremental(false);
    Properties props = new Properties();
    job.configureAnalysis(MONITOR, props, new ArrayList<SonarProperty>());

    assertThat(props.get(SonarProperties.SONAR_URL).toString(), is("http://localhost:9000"));
    assertThat(props.get(SonarProperties.PROJECT_KEY_PROPERTY).toString(), is("bar:foo"));
    assertThat(props.get(SonarProperties.DRY_RUN_PROPERTY).toString(), is("true"));
  }

  @Test
  public void shouldConfigureAnalysisAfter40() throws Exception {
    AnalyseProjectJob job = job(project);
    job.setIncremental(true);
    Properties props = new Properties();
    job.configureAnalysis(MONITOR, props, new ArrayList<SonarProperty>());

    assertThat(props.get(SonarProperties.SONAR_URL).toString(), is("http://localhost:9000"));
    assertThat(props.get(SonarProperties.PROJECT_KEY_PROPERTY).toString(), is("bar:foo"));
    assertThat(props.get(SonarProperties.ANALYSIS_MODE).toString(), is("incremental"));
  }

  @Test
  public void shouldConfigureAnalysisWithExtraProps() throws Exception {
    AnalyseProjectJob job = job(project);
    Properties props = new Properties();
    job.configureAnalysis(MONITOR, props, Arrays.asList(new SonarProperty("sonar.foo", "value")));

    assertThat(props.get("sonar.foo").toString(), is("value"));
  }

  @Test
  public void languageConfiguratorShouldOverrideExtraProps() throws Exception {
    AnalyseProjectJob job = job(project);
    Properties props = new Properties();
    job.configureAnalysis(MONITOR, props, Arrays.asList(new SonarProperty("sonar.language", "fake")));

    assertThat(props.get("sonar.language").toString(), is("java"));
  }

  @Test
  public void shouldCreateMarkersFromIssuesReport() throws Exception {
    AnalyseProjectJob job = job(project);
    job.createMarkersFromReportOutput(MONITOR, new File("testdata/sonar-report.json"));

    List<IMarker> markers = Arrays.asList(project.findMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE));
    assertThat(markers.size(), is(6));

    assertThat(markers, hasItem(new IsMarker("src/Findbugs.java", 5)));
    assertThat(markers, hasItem(new IsMarker("src/Pmd.java", 2)));
    assertThat(markers, hasItem(new IsMarker("src/Checkstyle.java", 1)));
  }

  @Test
  public void shouldCleanAndCreateMarkersFromIncrementalAnalysis() throws Exception {
    AnalyseProjectJob job = job(project);
    job.createMarkersFromReportOutput(MONITOR, new File("testdata/sonar-report.json"));

    // During incremental analysis PMD file was not modified, Findbugs has one remaing issue and Checkstyle has no remaining issues
    job = job(project);
    job.createMarkersFromReportOutput(MONITOR, new File("testdata/sonar-report-incremental.json"));

    List<IMarker> markers = Arrays.asList(project.findMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE));
    assertThat(markers.size(), is(3));

    assertThat(markers, hasItem(new IsMarker("src/Findbugs.java", 5)));
    assertThat(markers, hasItem(new IsMarker("src/Pmd.java", 2)));
  }

  static class IsMarker extends BaseMatcher<IMarker> {

    private final String path;
    private final int line;

    public IsMarker(String path, int line) {
      this.path = path;
      this.line = line;
    }

    public boolean matches(Object item) {
      IMarker marker = (IMarker) item;
      String actualPath = marker.getResource().getProjectRelativePath().toString();
      int actualLine = marker.getAttribute(IMarker.LINE_NUMBER, -1);
      return StringUtils.equals(actualPath, path) && (actualLine == line);
    }

    public void describeTo(Description description) {
      // TODO Auto-generated method stub
    }

  }

}
