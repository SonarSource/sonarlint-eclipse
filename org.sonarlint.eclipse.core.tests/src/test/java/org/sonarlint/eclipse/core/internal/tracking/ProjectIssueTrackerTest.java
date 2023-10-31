/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2023 SonarSource SA
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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.internal.resources.File;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.LogListener;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.serverconnection.ProjectBinding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProjectIssueTrackerTest {
  private List<String> debugs = new ArrayList<>();

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static final String DUMMY_FILE1_PATH = "dummyFile1";

  private PersistentLocalIssueStore store;

  private ProjectIssueTracker underTest;

  private ISonarLintFile file1;

  @Before
  public void setUp() throws IOException, URISyntaxException {
    var project = mock(ISonarLintProject.class);
    
    // In order for "ProjectIssueTracker.trackWithServerIssues(...)" to work we have to mock a project resource
    var dummyResource = mock(File.class);
    when(dummyResource.getLocationURI()).thenReturn(new URI(DUMMY_FILE1_PATH));
    when(project.getResource()).thenReturn(dummyResource);
    
    store = new PersistentLocalIssueStore(temporaryFolder.newFolder().toPath(), project);
    underTest = new ProjectIssueTracker(project, store);
    file1 = mock(ISonarLintFile.class);
    when(file1.getProjectRelativePath()).thenReturn(DUMMY_FILE1_PATH);
    
    SonarLintLogger.get().addLogListener(new LogListener() {
      @Override
      public void info(String msg, boolean fromAnalyzer) {
      }

      @Override
      public void error(String msg, boolean fromAnalyzer) {
      }

      @Override
      public void debug(String msg, boolean fromAnalyzer) {
        debugs.add(msg);
      }
    });
    
    debugs = new ArrayList<>();
  }

  @Test
  public void should_track_first_issues_as_unknown_creation_date() {
    var issue1 = mock(Issue.class);
    var issue2 = mock(Issue.class);
    underTest.processRawIssues(file1, List.of(new RawIssueTrackable(issue1), new RawIssueTrackable(issue2)));
    assertThat(underTest.getTracked(file1))
      .extracting(TrackedIssue::getIssueFromAnalysis, TrackedIssue::getCreationDate)
      .containsExactlyInAnyOrder(tuple(issue1, null), tuple(issue2, null));
  }

  @Test
  public void should_add_creation_date_for_new_issues_after_restart() throws IOException {
    store.save(DUMMY_FILE1_PATH, List.of());

    var now = System.currentTimeMillis();

    var issue1 = mock(Issue.class);
    var issue2 = mock(Issue.class);
    underTest.processRawIssues(file1, List.of(new RawIssueTrackable(issue1), new RawIssueTrackable(issue2)));
    assertThat(underTest.getTracked(file1))
      .extracting(TrackedIssue::getCreationDate)
      .allSatisfy(date -> assertThat(date).isGreaterThanOrEqualTo(now));
  }

  @Test
  public void should_add_creation_date_for_new_issues_after_first_analysis() {
    var issue1 = mock(Issue.class);
    when(issue1.getRuleKey()).thenReturn("rule1");
    when(issue1.getMessage()).thenReturn("Message issue1");
    underTest.processRawIssues(file1, List.of(new RawIssueTrackable(issue1)));

    var now = System.currentTimeMillis();

    var issue2 = mock(Issue.class);
    when(issue2.getRuleKey()).thenReturn("rule2");
    when(issue2.getMessage()).thenReturn("Message issue2");
    underTest.processRawIssues(file1, List.of(new RawIssueTrackable(issue2)));
    assertThat(underTest.getTracked(file1))
      .extracting(TrackedIssue::getCreationDate)
      .allSatisfy(date -> assertThat(date).isGreaterThanOrEqualTo(now));
  }

  @Test
  public void should_preserve_known_issues_with_null_date() {
    var issue1 = mock(Issue.class);
    when(issue1.getRuleKey()).thenReturn("rule1");
    when(issue1.getMessage()).thenReturn("Message issue1");
    underTest.processRawIssues(file1, List.of(new RawIssueTrackable(issue1)));

    var now = System.currentTimeMillis();

    var issue2 = mock(Issue.class);
    when(issue2.getRuleKey()).thenReturn("rule2");
    when(issue2.getMessage()).thenReturn("Message issue2");
    underTest.processRawIssues(file1, List.of(new RawIssueTrackable(issue1), new RawIssueTrackable(issue2)));
    assertThat(underTest.getTracked(file1))
      .extracting(TrackedIssue::getIssueFromAnalysis, TrackedIssue::getCreationDate)
      .satisfiesExactlyInAnyOrder(
        t -> {
          assertThat(t.toList().get(0)).isSameAs(issue1);
          assertThat(t.toList().get(1)).isNull();
        },
        t -> {
          assertThat(t.toList().get(0)).isSameAs(issue2);
          assertThat((long) t.toList().get(1)).isGreaterThanOrEqualTo(now);
        });
  }

  @Test
  public void should_drop_disappeared_issues() {
    var issue1 = mock(Issue.class);
    when(issue1.getRuleKey()).thenReturn("rule1");
    when(issue1.getMessage()).thenReturn("Message issue1");
    when(issue1.getStartLine()).thenReturn(1);

    var issue2 = mock(Issue.class);
    when(issue2.getRuleKey()).thenReturn("rule1");
    when(issue2.getMessage()).thenReturn("Message issue2");
    when(issue1.getStartLine()).thenReturn(2);

    underTest.processRawIssues(file1, List.of(new RawIssueTrackable(issue1), new RawIssueTrackable(issue2)));
    assertThat(underTest.getTracked(file1)).hasSize(2);

    // Second analysis, issue1 is gone
    underTest.processRawIssues(file1, List.of(new RawIssueTrackable(issue2)));

    assertThat(underTest.getTracked(file1)).extracting(TrackedIssue::getIssueFromAnalysis).containsOnly(issue2);
  }

  @Test
  public void should_not_match_issues_with_different_rule_key() {
    var issue1 = mock(Issue.class);
    when(issue1.getRuleKey()).thenReturn("rule1");
    when(issue1.getMessage()).thenReturn("Message issue");
    when(issue1.getStartLine()).thenReturn(1);

    underTest.processRawIssues(file1, List.of(new RawIssueTrackable(issue1)));

    var tracked1 = underTest.getTracked(file1).iterator().next();

    var issue2 = mock(Issue.class);
    when(issue2.getRuleKey()).thenReturn("rule2");
    when(issue2.getMessage()).thenReturn("Message issue");
    when(issue1.getStartLine()).thenReturn(1);

    underTest.processRawIssues(file1, List.of(new RawIssueTrackable(issue2)));

    var tracked2 = underTest.getTracked(file1).iterator().next();

    // tracked2 should be considered as a new issue, not as the same as tracked1
    assertThat(tracked2).isNotSameAs(tracked1);
  }

  @Test
  public void should_match_local_issues_by_text_range_hash() {
    var issue1 = mock(Issue.class);
    when(issue1.getRuleKey()).thenReturn("rule1");
    when(issue1.getMessage()).thenReturn("Message issue");
    when(issue1.getStartLine()).thenReturn(1);
    var rawIssue1 = new RawIssueTrackable(issue1, "// TODO", "  // TODO");

    underTest.processRawIssues(file1, List.of(rawIssue1));

    var movedIssue = mock(Issue.class);
    when(movedIssue.getRuleKey()).thenReturn("rule1");
    when(movedIssue.getMessage()).thenReturn("Message issue");
    when(movedIssue.getStartLine()).thenReturn(2);
    var movedRawIssue = new RawIssueTrackable(movedIssue, "// TODO", "  // TODO");

    var nonMatchingIssue = mock(Issue.class);
    when(nonMatchingIssue.getRuleKey()).thenReturn("rule1");
    when(nonMatchingIssue.getMessage()).thenReturn("Message issue");
    when(nonMatchingIssue.getStartLine()).thenReturn(1);
    var nonMatchingRawIssue = new RawIssueTrackable(nonMatchingIssue, "// TODO2", "  // TODO2");

    underTest.processRawIssues(file1, List.of(movedRawIssue, nonMatchingRawIssue));

    var trackedIssues = underTest.getTracked(file1);

    assertThat(trackedIssues).hasSize(2);

    var movedTracked = trackedIssues.stream().filter(t -> t.getIssueFromAnalysis() == movedIssue).findFirst().get();
    var nonMatchingTracked = trackedIssues.stream().filter(t -> t.getIssueFromAnalysis() == nonMatchingIssue).findFirst().get();

    // matched issue has no date
    assertThat(movedTracked.getCreationDate()).isNull();

    // unmatched issue has a date -> it is a leak
    assertThat(nonMatchingTracked.getCreationDate()).isNotNull();
  }

  /** Corner case in which the IDE path cannot be converted to server path */
  @Test
  public void test_trackWithServerIssues_path_null() {
    underTest.trackWithServerIssues(new ProjectBinding("testProject", "/", "/"), List.of(file1), false, new NullProgressMonitor());
    
    assertThat(debugs)
      .contains("'dummyFile1' cannot be converted from IDE to server path for project binding: 'testProject' / '/' / '/'");
  }
}
