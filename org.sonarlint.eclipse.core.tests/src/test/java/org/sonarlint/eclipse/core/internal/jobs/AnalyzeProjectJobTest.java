/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.core.internal.jobs;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProject;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.tests.common.SonarTestCase;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneAnalysisConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class AnalyzeProjectJobTest extends SonarTestCase {

  public org.junit.rules.ExternalResource test = null;
  private IProject project;
  private static final List<String> errors = new ArrayList<>();

  @BeforeClass
  public static void prepare() throws Exception {

    SonarLintCorePlugin.getDefault().addLogListener(new LogListener() {

      @Override
      public void info(String msg) {
      }

      @Override
      public void error(String msg) {
        errors.add(msg);
      }

      @Override
      public void debug(String msg) {
      }
    });
  }

  @Before
  public void cleanup() throws Exception {
    errors.clear();
    project = importEclipseProject("reference");
    MarkerUtils.deleteIssuesMarkers(project);

  }

  @After
  public void checkErrorsInLog() throws Exception {
    project.delete(true, null);
    if (!errors.isEmpty()) {
      fail(StringUtils.joinSkipNull(errors, "\n"));
    }
  }

  private static AnalyzeProjectJob job(IProject project, Collection<IFile> files) {
    return new AnalyzeProjectJob(new AnalyzeProjectRequest(project, files));
  }

  @Test
  public void run_first_analysis_with_one_issue() throws Exception {
    IFile file = project.getFile("src/Findbugs.java");
    AnalyzeProjectJob job = job(project, Arrays.asList(file));
    job = spy(job);
    Map<IResource, List<Issue>> result = new HashMap<>();
    Issue issue1 = mock(Issue.class);
    when(issue1.getRuleKey()).thenReturn("foo:bar");
    when(issue1.getSeverity()).thenReturn("BLOCKER");
    when(issue1.getMessage()).thenReturn("Self assignment of field");
    when(issue1.getStartLine()).thenReturn(5);
    when(issue1.getStartLineOffset()).thenReturn(4);
    when(issue1.getEndLine()).thenReturn(5);
    when(issue1.getEndLineOffset()).thenReturn(14);
    ClientInputFile inputFile = mock(ClientInputFile.class);
    when(inputFile.getClientObject()).thenReturn(file);
    when(issue1.getInputFile()).thenReturn(inputFile);
    result.put(file, Arrays.asList(issue1));
    doReturn(result).when(job).run(any(StandaloneAnalysisConfiguration.class), any(SonarLintProject.class), eq(MONITOR));
    job.runInWorkspace(MONITOR);

    IMarker[] markers = file.findMarkers(SonarLintCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE);
    assertThat(markers).hasSize(1);
    assertThat(markers[0].getAttribute(IMarker.LINE_NUMBER)).isEqualTo(5);
    assertThat(markers[0].getAttribute(IMarker.CHAR_START)).isEqualTo(78);
    assertThat(markers[0].getAttribute(IMarker.CHAR_END)).isEqualTo(88);
    assertThat(markers[0].getAttribute(MarkerUtils.SONAR_MARKER_CHECKSUM_ATTR)).isEqualTo("this.x=x".hashCode());
    String timestamp = (String) markers[0].getAttribute(MarkerUtils.SONAR_MARKER_CREATION_DATE_ATTR);
    assertThat(timestamp).isNotNull();
  }

  @Test
  public void issue_tracking() throws Exception {
    IFile file = project.getFile("src/Findbugs.java");
    AnalyzeProjectJob job = job(project, Arrays.asList(file));
    job = spy(job);
    Map<IResource, List<Issue>> result = new HashMap<>();
    Issue issue1 = mock(Issue.class);
    when(issue1.getRuleKey()).thenReturn("foo:bar");
    when(issue1.getSeverity()).thenReturn("BLOCKER");
    when(issue1.getMessage()).thenReturn("Self assignment of field");
    when(issue1.getStartLine()).thenReturn(5);
    when(issue1.getStartLineOffset()).thenReturn(4);
    when(issue1.getEndLine()).thenReturn(5);
    when(issue1.getEndLineOffset()).thenReturn(14);
    ClientInputFile inputFile = mock(ClientInputFile.class);
    when(inputFile.getClientObject()).thenReturn(file);
    when(issue1.getInputFile()).thenReturn(inputFile);
    result.put(file, Arrays.asList(issue1));
    doReturn(result).when(job).run(any(StandaloneAnalysisConfiguration.class), any(SonarLintProject.class), eq(MONITOR));
    job.runInWorkspace(MONITOR);
    IMarker[] markers = file.findMarkers(SonarLintCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE);
    assertThat(markers).hasSize(1);
    String timestamp = (String) markers[0].getAttribute(MarkerUtils.SONAR_MARKER_CREATION_DATE_ATTR);

    // Second execution same file, same issue
    job.runInWorkspace(MONITOR);

    markers = file.findMarkers(SonarLintCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE);
    assertThat(markers).hasSize(1);
    assertThat(markers[0].getAttribute(IMarker.LINE_NUMBER)).isEqualTo(5);
    assertThat(markers[0].getAttribute(IMarker.CHAR_START)).isEqualTo(78);
    assertThat(markers[0].getAttribute(IMarker.CHAR_END)).isEqualTo(88);
    assertThat(markers[0].getAttribute(MarkerUtils.SONAR_MARKER_CHECKSUM_ATTR)).isEqualTo("this.x=x".hashCode());
    assertThat(markers[0].getAttribute(MarkerUtils.SONAR_MARKER_CREATION_DATE_ATTR)).isEqualTo(timestamp);

    InputStream is = file.getContents();
    java.util.Scanner s = new java.util.Scanner(is, file.getCharset()).useDelimiter("\\A");
    String content = s.hasNext() ? s.next() : "";
    content = "\n\n" + content;
    file.setContents(new ByteArrayInputStream(content.getBytes(file.getCharset())), true, true, null);

    markers = file.findMarkers(SonarLintCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE);
    // Here marker was not notified of the file change
    assertThat(markers[0].getAttribute(IMarker.LINE_NUMBER)).isEqualTo(5);

    // Third execution with modified file content
    result = new HashMap<>();
    Issue issue1Updated = mock(Issue.class);
    when(issue1Updated.getRuleKey()).thenReturn("foo:bar");
    when(issue1Updated.getSeverity()).thenReturn("BLOCKER");
    when(issue1Updated.getMessage()).thenReturn("Self assignment of field");
    when(issue1Updated.getStartLine()).thenReturn(7);
    when(issue1Updated.getStartLineOffset()).thenReturn(4);
    when(issue1Updated.getEndLine()).thenReturn(7);
    when(issue1Updated.getEndLineOffset()).thenReturn(14);
    when(issue1Updated.getInputFile()).thenReturn(inputFile);
    result.put(file, Arrays.asList(issue1Updated));
    doReturn(result).when(job).run(any(StandaloneAnalysisConfiguration.class), any(SonarLintProject.class), eq(MONITOR));
    job.runInWorkspace(MONITOR);

    markers = file.findMarkers(SonarLintCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE);
    assertThat(markers).hasSize(1);
    assertThat(markers[0].getAttribute(IMarker.LINE_NUMBER)).isEqualTo(7);
    assertThat(markers[0].getAttribute(IMarker.CHAR_START)).isEqualTo(80);
    assertThat(markers[0].getAttribute(IMarker.CHAR_END)).isEqualTo(90);
    assertThat(markers[0].getAttribute(MarkerUtils.SONAR_MARKER_CHECKSUM_ATTR)).isEqualTo("this.x=x".hashCode());
    assertThat(markers[0].getAttribute(MarkerUtils.SONAR_MARKER_CREATION_DATE_ATTR)).isEqualTo(timestamp);
  }

}
