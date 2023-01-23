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

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.commons.IssueSeverity;
import org.sonarsource.sonarlint.core.commons.TextRange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IssueTrackerTest {

  private static final String DUMMY_FILE1_PATH = "dummyFile1";

  private final IssueTrackerCache cache = new InMemoryIssueTrackerCache();

  private final IssueTracker tracker = new IssueTracker(cache);

  private ISonarLintFile file1;

  // note: these mock trackables are used by many test cases,
  // with their line numbers to distinguish their identities.
  private final Trackable trackable1 = builder().line(1).build();
  private final Trackable trackable2 = builder().line(2).build();

  static class MockTrackableBuilder {
    String ruleKey = "";
    Integer line = null;
    String message = "";
    Integer textRangeHash = null;
    Integer lineHash = null;
    Long creationDate = null;
    String serverIssueKey = null;
    boolean resolved = false;
    IssueSeverity severity = IssueSeverity.MAJOR;
    IssueSeverity rawSeverity = IssueSeverity.MAJOR;

    static int counter = Integer.MIN_VALUE;

    MockTrackableBuilder ruleKey(String ruleKey) {
      this.ruleKey = ruleKey;
      return this;
    }

    MockTrackableBuilder line(Integer line) {
      this.line = line;
      return this;
    }

    MockTrackableBuilder message(String message) {
      this.message = message;
      return this;
    }

    MockTrackableBuilder textRangeHash(Integer textRangeHash) {
      this.textRangeHash = textRangeHash;
      return this;
    }

    MockTrackableBuilder lineHash(Integer lineHash) {
      this.lineHash = lineHash;
      return this;
    }

    MockTrackableBuilder creationDate(Long creationDate) {
      this.creationDate = creationDate;
      return this;
    }

    MockTrackableBuilder serverIssueKey(String serverIssueKey) {
      this.serverIssueKey = serverIssueKey;
      return this;
    }

    MockTrackableBuilder resolved(boolean resolved) {
      this.resolved = resolved;
      return this;
    }

    MockTrackableBuilder severity(IssueSeverity severity) {
      this.severity = severity;
      return this;
    }

    MockTrackableBuilder rawSeverity(IssueSeverity severity) {
      this.rawSeverity = severity;
      return this;
    }

    MockTrackableBuilder copy() {
      return builder()
        .line(line)
        .message(message)
        .textRangeHash(textRangeHash)
        .lineHash(lineHash)
        .ruleKey(ruleKey)
        .creationDate(creationDate)
        .serverIssueKey(serverIssueKey)
        .resolved(resolved)
        .severity(severity)
        .rawSeverity(rawSeverity);
    }

    Trackable build() {
      var trackable = mock(Trackable.class);
      when(trackable.getLine()).thenReturn(line);
      when(trackable.getTextRangeHash()).thenReturn(textRangeHash);
      when(trackable.getLineHash()).thenReturn(lineHash);
      when(trackable.getRuleKey()).thenReturn(ruleKey);
      when(trackable.getMessage()).thenReturn(message);
      when(trackable.getCreationDate()).thenReturn(creationDate);
      when(trackable.getServerIssueKey()).thenReturn(serverIssueKey);
      when(trackable.isResolved()).thenReturn(resolved);
      when(trackable.getSeverity()).thenReturn(severity);
      when(trackable.getRawSeverity()).thenReturn(rawSeverity);

      // set unique values for nullable fields used by the matchers in Tracker
      if (line == null) {
        when(trackable.getLine()).thenReturn(counter++);
      }
      if (lineHash == null) {
        when(trackable.getLineHash()).thenReturn(counter++);
      }
      if (textRangeHash == null) {
        when(trackable.getTextRangeHash()).thenReturn(counter++);
      }

      return trackable;
    }
  }

  private static MockTrackableBuilder builder() {
    return new MockTrackableBuilder();
  }

  private Issue mockIssue() {
    var issue = mock(Issue.class);
    when(issue.getRuleKey()).thenReturn("dummy ruleKey");
    when(issue.getMessage()).thenReturn("dummy message");
    return issue;
  }

  @Before
  public void setUp() {
    cache.clear();

    file1 = mock(ISonarLintFile.class);
    when(file1.getProjectRelativePath()).thenReturn(DUMMY_FILE1_PATH);
  }

  @Test
  public void should_track_first_trackables_exactly() {
    var trackables = List.of(mock(Trackable.class), mock(Trackable.class));
    assertThat(tracker.matchAndTrackAsNew(file1, trackables)).isEqualTo(trackables);
  }

  @Test
  public void should_preserve_known_standalone_trackables_with_null_date() {
    var trackables = List.of(trackable1, trackable2);
    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, trackables));
    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, trackables));

    var next = cache.getCurrentTrackables(DUMMY_FILE1_PATH);
    assertThat(next).extracting(t -> t.getLine()).containsExactlyInAnyOrder(trackable1.getLine(), trackable2.getLine());
    assertThat(next).extracting(t -> t.getCreationDate()).containsExactlyInAnyOrder(null, null);
  }

  @Test
  public void should_add_creation_date_for_leaked_trackables() {
    var start = System.currentTimeMillis();

    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, List.of(trackable1)));
    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, List.of(trackable1, trackable2)));

    var next = cache.getCurrentTrackables(DUMMY_FILE1_PATH);
    assertThat(next).extracting(t -> t.getLine()).contains(trackable1.getLine(), trackable2.getLine());

    assertThat(next).extracting(t -> t.getCreationDate()).containsOnlyOnce((Long) null);

    var leaked = next.stream().filter(t -> t.getCreationDate() != null).findFirst().get();
    assertThat(leaked.getCreationDate()).isGreaterThanOrEqualTo(start);
    assertThat(leaked.getLine()).isEqualTo(trackable2.getLine());
  }

  @Test
  public void should_drop_disappeared_issues() {
    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, List.of(trackable1, trackable2)));
    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, List.of(trackable1)));

    var next = cache.getCurrentTrackables(DUMMY_FILE1_PATH);
    assertThat(next).extracting(t -> t.getLine()).containsExactly(trackable1.getLine());
  }

  @Test
  public void should_not_match_trackables_with_different_rule_key() {
    var ruleKey = "dummy ruleKey";
    var base = builder()
      .line(7)
      .message("dummy message")
      .textRangeHash(11)
      .lineHash(13)
      .ruleKey(ruleKey)
      .serverIssueKey("dummy serverIssueKey")
      .creationDate(17L);

    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, List.of(base.build())));
    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, List.of(base.ruleKey(ruleKey + "x").build())));
  }

  @Test
  public void should_treat_new_issues_as_leak_when_old_issues_disappeared() {
    var start = System.currentTimeMillis();

    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, List.of(trackable1)));
    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, List.of(trackable2)));

    var next = cache.getCurrentTrackables(DUMMY_FILE1_PATH);
    assertThat(next).extracting(t -> t.getLine()).containsExactly(trackable2.getLine());

    var leaked = next.stream().filter(t -> t.getCreationDate() != null).findFirst().get();
    assertThat(leaked.getCreationDate()).isGreaterThanOrEqualTo(start);
  }

  @Test
  public void should_match_local_issues_by_line_hash() {
    var lineContent = "dummy content";
    var newLine = 7;

    var issue = mockIssue();
    when(issue.getStartLine()).thenReturn(newLine + 3);

    var movedIssue = mockIssue();
    when(movedIssue.getStartLine()).thenReturn(newLine);

    var trackable = new RawIssueTrackable(issue, mock(TextRange.class), null, lineContent);
    var movedTrackable = new RawIssueTrackable(movedIssue, mock(TextRange.class), null, lineContent);
    var nonMatchingTrackable = new RawIssueTrackable(mockIssue(), mock(TextRange.class), null, lineContent + "x");

    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, List.of(trackable)));
    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, List.of(movedTrackable, nonMatchingTrackable)));

    assertThat(movedTrackable.getLineHash()).isEqualTo(trackable.getLineHash());
    assertThat(movedTrackable.getLineHash()).isNotEqualTo(nonMatchingTrackable.getLineHash());

    var next = cache.getCurrentTrackables(DUMMY_FILE1_PATH);

    // matched trackable has no date
    assertThat(next.stream().filter(t -> t.getCreationDate() == null)).extracting("line", "lineHash").containsOnly(
      tuple(movedTrackable.getLine(), movedTrackable.getLineHash()));

    // unmatched trackable has a date -> it is a leak
    assertThat(next.stream().filter(t -> t.getCreationDate() != null)).extracting("line", "lineHash").containsOnly(
      tuple(nonMatchingTrackable.getLine(), nonMatchingTrackable.getLineHash()));
  }

  @Test
  public void should_preserve_creation_date_of_leaked_issues_in_connected_mode() {
    var leakCreationDate = 1L;
    var leak = builder().ruleKey("dummy ruleKey").line(7).textRangeHash(11).creationDate(leakCreationDate).build();

    // fake first analysis, trackable has a date
    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, List.of(leak)));

    // fake server issue tracking
    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackServerIssues(file1, List.of()));

    assertThat(cache.getCurrentTrackables(DUMMY_FILE1_PATH).iterator().next().getCreationDate()).isEqualTo(leakCreationDate);
  }

  @Test
  public void should_ignore_server_issues_when_there_are_no_local() {
    String serverIssueKey = "dummy serverIssueKey";
    boolean resolved = true;
    MockTrackableBuilder base = builder().ruleKey("dummy ruleKey").serverIssueKey(serverIssueKey).resolved(resolved);

    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, List.of()));
    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackServerIssues(file1, List.of(base.build())));

    assertThat(cache.getCurrentTrackables(DUMMY_FILE1_PATH)).isEmpty();
  }
}
