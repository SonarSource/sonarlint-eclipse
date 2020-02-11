/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2020 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.tracking;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.jobs.LogListener;
import org.sonarlint.eclipse.core.internal.jobs.SonarLintMarkerUpdater;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.markers.TextRange;
import org.sonarlint.eclipse.core.internal.resources.DefaultSonarLintFileAdapter;
import org.sonarlint.eclipse.core.internal.resources.DefaultSonarLintProjectAdapter;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.tests.common.SonarTestCase;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SonarLintMarkerUpdaterTest extends SonarTestCase {

  private static IProject project;
  private static final List<String> errors = new ArrayList<>();
  private DefaultSonarLintFileAdapter sonarLintFile;

  @BeforeClass
  public static void prepare() throws Exception {
    SonarLintLogger.get().addLogListener(new LogListener() {
      @Override
      public void info(String msg, boolean fromAnalyzer) {
      }

      @Override
      public void error(String msg, boolean fromAnalyzer) {
        errors.add(msg);
      }

      @Override
      public void debug(String msg, boolean fromAnalyzer) {
      }

    });
    project = importEclipseProject("reference");
  }

  @Before
  public void cleanup() throws Exception {
    errors.clear();
  }

  @After
  public void checkErrorsInLog() throws Exception {
    if (!errors.isEmpty()) {
      fail(StringUtils.joinSkipNull(errors, "\n"));
    }
  }

  private IMarker[] processTrackable(Trackable... trackables) throws CoreException {
    String relativePath = "src/Findbugs.java";
    String absolutePath = project.getLocation().toString() + "/" + relativePath;
    IPath location = Path.fromOSString(absolutePath);
    IFile file = workspace.getRoot().getFileForLocation(location);
    sonarLintFile = new DefaultSonarLintFileAdapter(new DefaultSonarLintProjectAdapter(project), file);
    sonarLintFile = spy(sonarLintFile);
    SonarLintMarkerUpdater.createOrUpdateMarkers(sonarLintFile, Optional.empty(), asList(trackables), TriggerType.EDITOR_CHANGE);

    return project.getFile(relativePath).findMarkers(SonarLintCorePlugin.MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_INFINITE);
  }

  /**
   * Create a mock trackable with valid mandatory values and correct defaults.
   *
   * @return a mock trackable
   */
  private Trackable newMockTrackable() {
    Trackable trackable = mock(Trackable.class);
    // mandatory non-nulls
    when(trackable.getTextRange()).thenReturn(TextRange.get(1));
    when(trackable.getSeverity()).thenReturn("");

    // explicit nulls, because Mockito uses 0 values otherwise
    when(trackable.getLine()).thenReturn(null);
    when(trackable.getCreationDate()).thenReturn(null);
    return trackable;
  }

  @Test
  public void test_marker_of_ordinary_trackable() throws Exception {
    Trackable trackable = newMockTrackable();

    int priority = 2;
    String severity = "BLOCKER";
    int eclipseSeverity = 0;
    when(trackable.getSeverity()).thenReturn(severity);

    String message = "Self assignment of field";
    when(trackable.getMessage()).thenReturn(message);

    String serverIssueKey = "dummy-serverIssueKey";
    when(trackable.getServerIssueKey()).thenReturn(serverIssueKey);

    String assignee = "admin";
    when(trackable.getAssignee()).thenReturn(assignee);

    IMarker[] markers = processTrackable(trackable);
    assertThat(markers).hasSize(1);
    assertThat(markers[0].getAttribute(IMarker.PRIORITY)).isEqualTo(priority);
    assertThat(markers[0].getAttribute(IMarker.SEVERITY)).isEqualTo(eclipseSeverity);
    assertThat(markers[0].getAttribute(MarkerUtils.SONAR_MARKER_ISSUE_SEVERITY_ATTR)).isEqualTo(severity);
    assertThat(markers[0].getAttribute(IMarker.MESSAGE)).isEqualTo(message);
    assertThat(markers[0].getAttribute(MarkerUtils.SONAR_MARKER_SERVER_ISSUE_KEY_ATTR)).isEqualTo(serverIssueKey);
  }

  @Test
  public void test_marker_of_trackable_with_text_range() throws Exception {
    Trackable trackable = newMockTrackable();

    int line = 5;
    when(trackable.getLine()).thenReturn(line);
    when(trackable.getTextRange()).thenReturn(TextRange.get(line, 4, 5, 14));

    IMarker[] markers = processTrackable(trackable);
    assertThat(markers).hasSize(1);

    assertThat(markers[0].getAttribute(IMarker.LINE_NUMBER)).isEqualTo(5);
    assertThat(markers[0].getAttribute(IMarker.CHAR_START)).isEqualTo(78);
    assertThat(markers[0].getAttribute(IMarker.CHAR_END)).isEqualTo(88);
  }

  @Test
  public void dont_init_document_if_no_issues() throws Exception {
    Trackable trackable = newMockTrackable();

    int line = 5;
    when(trackable.getLine()).thenReturn(line);
    when(trackable.getTextRange()).thenReturn(TextRange.get(line, 4, 5, 14));

    IMarker[] markers = processTrackable();
    assertThat(markers).isEmpty();

    verify(sonarLintFile, never()).getDocument();
  }

  @Test
  public void init_document_only_once_if_multiple_issues() throws Exception {
    Trackable trackable1 = newMockTrackable();

    int line1 = 5;
    when(trackable1.getLine()).thenReturn(line1);
    when(trackable1.getTextRange()).thenReturn(TextRange.get(line1, 4, 5, 14));

    Trackable trackable2 = newMockTrackable();

    int line2 = 4;
    when(trackable2.getLine()).thenReturn(line2);
    when(trackable2.getTextRange()).thenReturn(TextRange.get(line2, 4, 5, 14));

    IMarker[] markers = processTrackable(trackable1, trackable2);
    assertThat(markers).hasSize(2);

    verify(sonarLintFile, times(1)).getDocument();
  }

  @Test
  public void test_marker_of_trackable_with_line() throws Exception {
    Trackable trackable = newMockTrackable();

    int line = 5;
    when(trackable.getLine()).thenReturn(line);
    when(trackable.getTextRange()).thenReturn(TextRange.get(line, 4, 5, 14));

    IMarker[] markers = processTrackable(trackable);
    assertThat(markers).hasSize(1);

    assertThat(markers[0].getAttribute(IMarker.LINE_NUMBER)).isEqualTo(5);
    assertThat(markers[0].getAttribute(IMarker.CHAR_START)).isEqualTo(78);
    assertThat(markers[0].getAttribute(IMarker.CHAR_END)).isEqualTo(88);
  }

  @Test
  public void test_marker_of_trackable_without_line() throws Exception {
    Trackable trackable = newMockTrackable();
    IMarker[] markers = processTrackable(trackable);
    assertThat(markers).hasSize(1);
    assertThat(markers[0].getAttribute(IMarker.LINE_NUMBER)).isEqualTo(1);
  }

  @Test
  public void test_marker_of_trackable_with_creation_date() throws Exception {
    Trackable trackable = newMockTrackable();

    long creationDate = System.currentTimeMillis();
    when(trackable.getCreationDate()).thenReturn(creationDate);

    IMarker[] markers = processTrackable(trackable);
    assertThat(markers).hasSize(1);
    assertThat(markers[0].getAttribute(MarkerUtils.SONAR_MARKER_CREATION_DATE_ATTR)).isEqualTo(Long.toString(creationDate));
  }

  @Test
  public void test_marker_of_trackable_without_creation_date() throws Exception {
    Trackable trackable = newMockTrackable();
    IMarker[] markers = processTrackable(trackable);
    assertThat(markers).hasSize(1);
    assertThat(markers[0].getAttribute(MarkerUtils.SONAR_MARKER_CREATION_DATE_ATTR)).isNull();
  }
}
