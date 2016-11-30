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
package org.sonarlint.eclipse.core.internal.tracking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.jobs.LogListener;
import org.sonarlint.eclipse.core.internal.jobs.MarkerUpdaterCallable;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.markers.TextRange;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.tests.common.SonarTestCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MarkerUpdaterTest extends SonarTestCase {

  private static IProject project;
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

      @Override
      public void warn(String msg) {
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

  private IMarker processTrackable(Trackable trackable) throws CoreException {
    String relativePath = "src/Findbugs.java";
    String absolutePath = project.getLocation().toString() + "/" + relativePath;
    IPath location = Path.fromOSString(absolutePath);
    IFile file = workspace.getRoot().getFileForLocation(location);
    MarkerUpdaterCallable markerUpdater = new MarkerUpdaterCallable(file, Collections.singletonList(trackable), TriggerType.EDITOR_CHANGE);

    markerUpdater.call();

    IMarker[] markers = project.getFile(relativePath).findMarkers(SonarLintCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE);
    assertThat(markers).hasSize(1);

    return markers[0];
  }

  /**
   * Create a mock trackable with valid mandatory values and correct defaults.
   *
   * @return a mock trackable
   */
  private Trackable newMockTrackable() {
    Trackable trackable = mock(Trackable.class);
    // mandatory non-nulls
    when(trackable.getTextRange()).thenReturn(new TextRange(1));
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

    IMarker marker = processTrackable(trackable);
    assertThat(marker.getAttribute(IMarker.PRIORITY)).isEqualTo(priority);
    assertThat(marker.getAttribute(IMarker.SEVERITY)).isEqualTo(eclipseSeverity);
    assertThat(marker.getAttribute(MarkerUtils.SONAR_MARKER_ISSUE_SEVERITY_ATTR)).isEqualTo(severity);
    assertThat(marker.getAttribute(IMarker.MESSAGE)).isEqualTo(message);
    assertThat(marker.getAttribute(MarkerUtils.SONAR_MARKER_SERVER_ISSUE_KEY_ATTR)).isEqualTo(serverIssueKey);
  }

  @Test
  public void test_marker_of_trackable_with_text_range() throws Exception {
    Trackable trackable = newMockTrackable();

    int line = 5;
    when(trackable.getLine()).thenReturn(line);
    when(trackable.getTextRange()).thenReturn(new TextRange(line, 4, 5, 14));

    IMarker marker = processTrackable(trackable);

    assertThat(marker.getAttribute(IMarker.LINE_NUMBER)).isEqualTo(5);
    assertThat(marker.getAttribute(IMarker.CHAR_START)).isEqualTo(78);
    assertThat(marker.getAttribute(IMarker.CHAR_END)).isEqualTo(88);
  }

  @Test
  public void test_marker_of_trackable_with_line() throws Exception {
    Trackable trackable = newMockTrackable();

    int line = 5;
    when(trackable.getLine()).thenReturn(line);
    when(trackable.getTextRange()).thenReturn(new TextRange(line, 4, 5, 14));

    IMarker marker = processTrackable(trackable);

    assertThat(marker.getAttribute(IMarker.LINE_NUMBER)).isEqualTo(5);
    assertThat(marker.getAttribute(IMarker.CHAR_START)).isEqualTo(78);
    assertThat(marker.getAttribute(IMarker.CHAR_END)).isEqualTo(88);
  }

  @Test
  public void test_marker_of_trackable_without_line() throws Exception {
    Trackable trackable = newMockTrackable();
    IMarker marker = processTrackable(trackable);
    assertThat(marker.getAttribute(IMarker.LINE_NUMBER)).isEqualTo(1);
  }

  @Test
  public void test_marker_of_trackable_with_creation_date() throws Exception {
    Trackable trackable = newMockTrackable();

    long creationDate = System.currentTimeMillis();
    when(trackable.getCreationDate()).thenReturn(creationDate);

    IMarker marker = processTrackable(trackable);
    assertThat(marker.getAttribute(MarkerUtils.SONAR_MARKER_CREATION_DATE_ATTR)).isEqualTo(Long.toString(creationDate));
  }

  @Test
  public void test_marker_of_trackable_without_creation_date() throws Exception {
    Trackable trackable = newMockTrackable();
    IMarker marker = processTrackable(trackable);
    assertThat(marker.getAttribute(MarkerUtils.SONAR_MARKER_CREATION_DATE_ATTR)).isNull();
  }
}
