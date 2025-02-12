/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.LogListener;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.jobs.SonarLintMarkerUpdater;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.resources.DefaultSonarLintFileAdapter;
import org.sonarlint.eclipse.core.internal.resources.DefaultSonarLintProjectAdapter;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.tests.common.SonarTestCase;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.ImpactDto;
import org.sonarsource.sonarlint.core.rpc.protocol.client.issue.RaisedIssueDto;
import org.sonarsource.sonarlint.core.rpc.protocol.common.CleanCodeAttribute;
import org.sonarsource.sonarlint.core.rpc.protocol.common.Either;
import org.sonarsource.sonarlint.core.rpc.protocol.common.ImpactSeverity;
import org.sonarsource.sonarlint.core.rpc.protocol.common.IssueSeverity;
import org.sonarsource.sonarlint.core.rpc.protocol.common.MQRModeDetails;
import org.sonarsource.sonarlint.core.rpc.protocol.common.RuleType;
import org.sonarsource.sonarlint.core.rpc.protocol.common.SoftwareQuality;
import org.sonarsource.sonarlint.core.rpc.protocol.common.StandardModeDetails;
import org.sonarsource.sonarlint.core.rpc.protocol.common.TextRangeDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
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
        // We ignore info messages in UTs
      }

      @Override
      public void error(String msg, boolean fromAnalyzer) {
        // XXX Ugly hack to ignore asynchronous error messages from the backend
        if (!msg.contains("Attempt to update binding in configuration scope ")
          && !msg.contains("org.sonarsource.sonarlint.core.serverconnection.storage.StorageException: Failed to read file:")) {
          errors.add(msg);
        }
      }

      @Override
      public void error(@Nullable String msg, Throwable t, boolean fromAnalyzer) {
        var stack = new StringWriter();
        t.printStackTrace(new PrintWriter(stack));
        error(msg, fromAnalyzer);
        error(stack.toString(), fromAnalyzer);
      }

      @Override
      public void debug(String msg, boolean fromAnalyzer) {
        // We ignore debug messages in UTs
      }

      @Override
      public void debug(@Nullable String msg, Throwable t, boolean fromAnalyzer) {
        // We ignore debug messages in UTs
      }

      @Override
      public void traceIdeMessage(@Nullable String msg) {
        // INFO: We ignore Eclipse-specific tracing in UTs
      }

      @Override
      public void traceIdeMessage(@Nullable String msg, Throwable t) {
        // INFO: We ignore Eclipse-specific tracing in UTs
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

  private IMarker[] processRaisedIssueDto(RaisedIssueDto... issues) throws CoreException {
    var relativePath = "src/Findbugs.java";
    var absolutePath = project.getLocation().toString() + "/" + relativePath;
    var location = Path.fromOSString(absolutePath);
    var file = workspace.getRoot().getFileForLocation(location);
    sonarLintFile = new DefaultSonarLintFileAdapter(new DefaultSonarLintProjectAdapter(project), file);
    sonarLintFile = spy(sonarLintFile);
    SonarLintMarkerUpdater.createOrUpdateMarkers(sonarLintFile, List.of(issues),
      TriggerType.EDITOR_CHANGE.isOnTheFly(), false, false, true);

    return project.getFile(relativePath).findMarkers(SonarLintCorePlugin.MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_INFINITE);
  }

  private RaisedIssueDto newMockRaisedIssueDto() {
    var issue = mock(RaisedIssueDto.class);
    when(issue.getId()).thenReturn(UUID.randomUUID());
    when(issue.getTextRange()).thenReturn(new TextRangeDto(1, 2, 3, 4));
    when(issue.getIntroductionDate()).thenReturn(Instant.now());
    return issue;
  }

  @Test
  public void test_marker_of_ordinary_trackable() throws Exception {
    var issue = newMockRaisedIssueDto();

    var priority = 2;
    var severity = IssueSeverity.BLOCKER;
    var type = RuleType.BUG;
    var eclipseSeverity = 0;

    when(issue.getSeverityMode()).thenReturn(Either.forLeft(new StandardModeDetails(severity, type)));

    var message = "Self assignment of field";
    when(issue.getPrimaryMessage()).thenReturn(message);

    var serverIssueKey = "dummy-serverIssueKey";
    when(issue.getServerKey()).thenReturn(serverIssueKey);

    var id = UUID.randomUUID();
    when(issue.getId()).thenReturn(id);

    var resolved = false;
    when(issue.isResolved()).thenReturn(resolved);

    var markers = processRaisedIssueDto(issue);
    assertThat(markers).hasSize(1);
    assertThat(markers[0].getAttribute(IMarker.PRIORITY)).isEqualTo(priority);
    assertThat(markers[0].getAttribute(MarkerUtils.SONAR_MARKER_RULE_DESC_CONTEXT_KEY_ATTR)).isNull();
    assertThat(markers[0].getAttribute(IMarker.SEVERITY)).isEqualTo(eclipseSeverity);
    assertThat(markers[0].getAttribute(MarkerUtils.SONAR_MARKER_ISSUE_SEVERITY_ATTR)).isEqualTo(severity);
    assertThat(markers[0].getAttribute(IMarker.MESSAGE)).isEqualTo(message);
    assertThat(markers[0].getAttribute(MarkerUtils.SONAR_MARKER_SERVER_ISSUE_KEY_ATTR)).isEqualTo(serverIssueKey);
    assertThat(markers[0].getAttribute(MarkerUtils.SONAR_MARKER_TRACKED_ISSUE_ID_ATTR)).isEqualTo(id.toString());
    assertThat(markers[0].getAttribute(MarkerUtils.SONAR_MARKER_RESOLVED_ATTR)).isEqualTo(false);
    assertThat(markers[0].getAttribute(MarkerUtils.SONAR_MARKER_ANTICIPATED_ISSUE_ATTR)).isEqualTo(true);
  }

  @Test
  public void test_marker_of_trackable_with_text_range() throws Exception {
    var issue = newMockRaisedIssueDto();

    var severity = IssueSeverity.MINOR;
    var type = RuleType.BUG;
    when(issue.getSeverityMode()).thenReturn(Either.forLeft(new StandardModeDetails(severity, type)));

    when(issue.getTextRange()).thenReturn(new TextRangeDto(5, 4, 5, 14));

    var markers = processRaisedIssueDto(issue);
    assertThat(markers).hasSize(1);

    assertThat(markers[0].getAttribute(IMarker.LINE_NUMBER)).isEqualTo(5);
    assertThat(markers[0].getAttribute(IMarker.CHAR_START)).isEqualTo(78);
    assertThat(markers[0].getAttribute(IMarker.CHAR_END)).isEqualTo(88);
  }

  @Test
  public void test_marker_of_trackable_with_rule_context() throws Exception {
    var issue = newMockRaisedIssueDto();

    var severity = IssueSeverity.BLOCKER;
    var type = RuleType.BUG;
    when(issue.getSeverityMode()).thenReturn(Either.forLeft(new StandardModeDetails(severity, type)));

    when(issue.getRuleDescriptionContextKey()).thenReturn("struts");

    var markers = processRaisedIssueDto(issue);
    assertThat(markers).hasSize(1);

    assertThat(markers[0].getAttribute(MarkerUtils.SONAR_MARKER_RULE_DESC_CONTEXT_KEY_ATTR)).isEqualTo("struts");
  }

  @Test
  public void test_marker_of_trackable_with_line() throws Exception {
    var issue = newMockRaisedIssueDto();

    var cleanCodeAttribute = CleanCodeAttribute.CLEAR;
    var impacts = List.of(new ImpactDto(SoftwareQuality.MAINTAINABILITY, ImpactSeverity.INFO),
      new ImpactDto(SoftwareQuality.RELIABILITY, ImpactSeverity.MEDIUM));

    when(issue.getSeverityMode()).thenReturn(Either.forRight(new MQRModeDetails(cleanCodeAttribute, impacts)));

    when(issue.getTextRange()).thenReturn(new TextRangeDto(5, 4, 5, 14));

    var markers = processRaisedIssueDto(issue);
    assertThat(markers).hasSize(1);

    assertThat(markers[0].getAttribute(IMarker.LINE_NUMBER)).isEqualTo(5);
    assertThat(markers[0].getAttribute(IMarker.CHAR_START)).isEqualTo(78);
    assertThat(markers[0].getAttribute(IMarker.CHAR_END)).isEqualTo(88);
  }

  @Test
  public void test_marker_of_trackable_without_line() throws Exception {
    var issue = newMockRaisedIssueDto();

    var cleanCodeAttribute = CleanCodeAttribute.CLEAR;
    var impacts = List.of(new ImpactDto(SoftwareQuality.MAINTAINABILITY, ImpactSeverity.INFO));

    when(issue.getSeverityMode()).thenReturn(Either.forRight(new MQRModeDetails(cleanCodeAttribute, impacts)));

    var markers = processRaisedIssueDto(issue);
    assertThat(markers).hasSize(1);
    assertThat(markers[0].getAttribute(IMarker.LINE_NUMBER)).isEqualTo(1);
  }

  @Test
  public void test_marker_of_trackable_with_creation_date() throws Exception {
    var issue = newMockRaisedIssueDto();

    var cleanCodeAttribute = CleanCodeAttribute.CLEAR;
    var impacts = List.of(new ImpactDto(SoftwareQuality.MAINTAINABILITY, ImpactSeverity.INFO),
      new ImpactDto(SoftwareQuality.RELIABILITY, ImpactSeverity.HIGH));

    when(issue.getSeverityMode()).thenReturn(Either.forRight(new MQRModeDetails(cleanCodeAttribute, impacts)));

    var introduction = Instant.now();
    when(issue.getIntroductionDate()).thenReturn(introduction);

    var markers = processRaisedIssueDto(issue);
    assertThat(markers).hasSize(1);
    assertThat(markers[0].getAttribute(MarkerUtils.SONAR_MARKER_CREATION_DATE_ATTR))
      .isEqualTo(Long.toString(introduction.toEpochMilli()));
  }
}
