/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2021 SonarSource SA
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

import java.time.Instant;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.sonarlint.eclipse.core.internal.markers.TextRange;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.connected.ServerIssue;

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
    String assignee = "";
    String severity = "MAJOR";
    String rawSeverity = "MAJOR";

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

    MockTrackableBuilder assignee(String assignee) {
      this.assignee = assignee;
      return this;
    }

    MockTrackableBuilder severity(String severity) {
      this.severity = severity;
      return this;
    }

    MockTrackableBuilder rawSeverity(String severity) {
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
        .assignee(assignee)
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
      when(trackable.getAssignee()).thenReturn(assignee);
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
      .creationDate(17L)
      .assignee("dummy assignee");

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
  public void should_match_by_line_and_text_range_hash() {
    var base = builder().ruleKey("dummy ruleKey");
    var line = 7;
    var textRangeHash = 11;
    // note: (ab)using the assignee field to uniquely identify the trackable
    var id = "dummy id";
    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, List.of(base.copy().line(line).textRangeHash(textRangeHash).assignee(id).build())));

    var differentLine = base.line(line + 1).textRangeHash(textRangeHash).build();
    var differentTextRangeHash = base.line(line).textRangeHash(textRangeHash + 1).build();
    var differentBoth = base.line(line + 1).textRangeHash(textRangeHash + 1).build();
    var same = base.line(line).textRangeHash(textRangeHash).build();
    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, List.of(differentLine, differentTextRangeHash, differentBoth, same)));

    var current = cache.getCurrentTrackables(DUMMY_FILE1_PATH);
    assertThat(current).hasSize(4);
    assertThat(current).extracting("assignee").containsOnlyOnce(id);
    assertThat(current).extracting("line", "textRangeHash", "assignee").containsOnlyOnce(tuple(line, textRangeHash, id));
  }

  @Test
  public void should_match_by_line_and_message() {
    var base = builder().ruleKey("dummy ruleKey");
    var line = 7;
    var message = "should make this condition not always false";
    // note: (ab)using the assignee field to uniquely identify the trackable
    var id = "dummy id";
    var c = 1;
    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, List.of(base.copy().line(line).message(message).assignee(id).textRangeHash(c++).build())));

    var differentLine = base.line(line + 1).message(message).textRangeHash(c++).build();
    var differentMessage = base.line(line).message(message + "x").textRangeHash(c++).build();
    var differentBoth = base.line(line + 1).message(message + "x").textRangeHash(c++).build();
    var same = base.line(line).message(message).textRangeHash(c++).build();
    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, List.of(differentLine, differentMessage, differentBoth, same)));

    var current = cache.getCurrentTrackables(DUMMY_FILE1_PATH);
    assertThat(current).hasSize(4);
    assertThat(current).extracting("assignee").containsOnlyOnce(id);
    assertThat(current).extracting("line", "message", "assignee").containsOnlyOnce(tuple(line, message, id));
  }

  @Test
  public void should_match_by_text_range_hash() {
    var base = builder().ruleKey("dummy ruleKey").textRangeHash(11);
    // note: (ab)using the assignee field to uniquely identify the trackable
    var id = "dummy id";
    var newLine = 7;

    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, List.of(base.copy().line(newLine + 3).assignee(id).build())));
    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, List.of(base.line(newLine).build())));

    assertThat(cache.getCurrentTrackables(DUMMY_FILE1_PATH)).extracting("line", "assignee").containsExactly(tuple(newLine, id));
  }

  @Test
  public void should_match_by_line_hash() {
    var base = builder().ruleKey("dummy ruleKey").lineHash(11);
    // note: (ab)using the assignee field to uniquely identify the trackable
    var id = "dummy id";
    var newLine = 7;

    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, List.of(base.copy().line(newLine + 3).assignee(id).build())));
    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, List.of(base.line(newLine).build())));

    assertThat(cache.getCurrentTrackables(DUMMY_FILE1_PATH)).extracting("line", "assignee").containsExactly(tuple(newLine, id));
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
  public void should_match_server_issues_by_line_hash() {
    var ruleKey = "dummy ruleKey";
    var message = "dummy message";
    var lineContent = "dummy content";
    var newLine = 7;
    var serverIssueKey = "dummy serverIssueKey";

    var issue = mock(Issue.class);
    when(issue.getRuleKey()).thenReturn(ruleKey);
    when(issue.getMessage()).thenReturn(message);
    when(issue.getStartLine()).thenReturn(newLine);
    var trackable = new RawIssueTrackable(issue, mock(TextRange.class), null, lineContent);

    var serverIssue = mock(ServerIssue.class);
    when(serverIssue.ruleKey()).thenReturn(ruleKey);
    when(serverIssue.getMessage()).thenReturn(message);
    when(serverIssue.lineHash()).thenReturn(DigestUtils.digest(lineContent));
    when(serverIssue.getStartLine()).thenReturn(newLine + 3);
    when(serverIssue.creationDate()).thenReturn(Instant.now());
    when(serverIssue.key()).thenReturn(serverIssueKey);
    when(serverIssue.resolution()).thenReturn("fixed");
    var movedTrackable = new ServerIssueTrackable(serverIssue);

    var nonMatchingTrackable = new RawIssueTrackable(mockIssue(), mock(TextRange.class), null, lineContent + "x");

    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, List.of(trackable)));
    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackServerIssues(file1, List.of(movedTrackable, nonMatchingTrackable)));

    assertThat(movedTrackable.getLineHash()).isEqualTo(trackable.getLineHash());
    assertThat(movedTrackable.getLineHash()).isNotEqualTo(nonMatchingTrackable.getLineHash());

    assertThat(cache.getCurrentTrackables(DUMMY_FILE1_PATH)).extracting("line", "lineHash", "serverIssueKey", "resolved").containsOnly(
      tuple(newLine, movedTrackable.getLineHash(), movedTrackable.getServerIssueKey(), movedTrackable.isResolved()));
  }

  @Test
  public void should_match_first_by_server_issue_key() {
    var ruleKey = "dummy ruleKey";
    var message = "dummy message";
    var lineContent = "dummy content";
    var serverIssueKey1 = "dummy serverIssueKey 1";
    var serverIssueKey2 = "dummy serverIssueKey 2";

    var issue1 = mock(Issue.class);
    when(issue1.getRuleKey()).thenReturn(ruleKey);
    when(issue1.getMessage()).thenReturn(message);
    when(issue1.getStartLine()).thenReturn(1);
    var trackable1 = new RawIssueTrackable(issue1, mock(TextRange.class), null, lineContent);

    var issue2 = mock(Issue.class);
    when(issue2.getRuleKey()).thenReturn(ruleKey);
    when(issue2.getMessage()).thenReturn(message);
    when(issue2.getStartLine()).thenReturn(2);
    var trackable2 = new RawIssueTrackable(issue2, mock(TextRange.class), null, lineContent);

    var serverIssue1 = mock(ServerIssue.class);
    when(serverIssue1.ruleKey()).thenReturn(ruleKey);
    when(serverIssue1.getMessage()).thenReturn(message);
    when(serverIssue1.lineHash()).thenReturn(DigestUtils.digest(lineContent));
    when(serverIssue1.getStartLine()).thenReturn(1);
    when(serverIssue1.creationDate()).thenReturn(Instant.now());
    when(serverIssue1.key()).thenReturn(serverIssueKey1);
    when(serverIssue1.resolution()).thenReturn("");
    var serverTrackable1 = new ServerIssueTrackable(serverIssue1);

    var serverIssue2 = mock(ServerIssue.class);
    when(serverIssue2.ruleKey()).thenReturn(ruleKey);
    when(serverIssue2.getMessage()).thenReturn(message);
    when(serverIssue2.lineHash()).thenReturn(DigestUtils.digest(lineContent));
    when(serverIssue2.getStartLine()).thenReturn(2);
    when(serverIssue2.creationDate()).thenReturn(Instant.now());
    when(serverIssue2.key()).thenReturn(serverIssueKey2);
    when(serverIssue2.resolution()).thenReturn("");
    var serverTrackable2 = new ServerIssueTrackable(serverIssue2);

    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, List.of(trackable1, trackable2)));
    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackServerIssues(file1, List.of(serverTrackable1, serverTrackable2)));

    assertThat(cache.getCurrentTrackables(DUMMY_FILE1_PATH)).extracting("line", "lineHash", "serverIssueKey").containsOnly(
      tuple(1, serverTrackable1.getLineHash(), serverTrackable1.getServerIssueKey()),
      tuple(2, serverTrackable2.getLineHash(), serverTrackable2.getServerIssueKey()));

    // Emulate code shifted by one line
    when(issue1.getStartLine()).thenReturn(2);
    when(issue2.getStartLine()).thenReturn(3);

    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, List.of(trackable1, trackable2)));
    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackServerIssues(file1, List.of(serverTrackable1, serverTrackable2)));

    assertThat(cache.getCurrentTrackables(DUMMY_FILE1_PATH)).extracting("line", "lineHash", "serverIssueKey").containsOnly(
      tuple(2, serverTrackable1.getLineHash(), serverTrackable1.getServerIssueKey()),
      tuple(3, serverTrackable2.getLineHash(), serverTrackable2.getServerIssueKey()));
  }

  @Test
  public void should_preserve_creation_date() {
    var base = builder().ruleKey("dummy ruleKey").line(7).textRangeHash(11);
    // note: (ab)using the assignee field to uniquely identify the trackable
    var id = "dummy id";
    var creationDate = 123L;

    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, List.of(base.copy().creationDate(creationDate).assignee(id).build())));
    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, List.of(base.build())));

    assertThat(cache.getCurrentTrackables(DUMMY_FILE1_PATH)).extracting("creationDate", "assignee").containsExactly(tuple(creationDate, id));
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
  public void should_preserve_server_issue_details() {
    var base = builder().ruleKey("dummy ruleKey").line(7).textRangeHash(11).severity("CRITICAL");
    // note: (ab)using the assignee field to uniquely identify the trackable
    var id = "dummy id";
    var serverIssueKey = "dummy serverIssueKey";
    var resolved = true;

    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, List.of(base.build())));
    cache.put(DUMMY_FILE1_PATH,
      tracker.matchAndTrackServerIssues(file1, List.of(base.copy().serverIssueKey(serverIssueKey).resolved(resolved).assignee(id).severity("BLOCKER").build())));

    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, List.of(base.build())));

    assertThat(cache.getCurrentTrackables(DUMMY_FILE1_PATH)).extracting("serverIssueKey", "resolved", "assignee", "severity")
      .containsExactly(tuple(serverIssueKey, resolved, id, "BLOCKER"));
  }

  @Test
  public void should_update_server_issue_details() {
    var serverIssueKey = "dummy serverIssueKey";
    var resolved = true;
    var assignee = "dummy assignee";
    var base = builder().ruleKey("dummy ruleKey").serverIssueKey(serverIssueKey).resolved(resolved).assignee(assignee);

    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, List.of(base.copy().resolved(!resolved).assignee(assignee + "x").build())));
    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackServerIssues(file1, List.of(base.build())));

    assertThat(cache.getCurrentTrackables(DUMMY_FILE1_PATH)).extracting("serverIssueKey", "resolved", "assignee").containsExactly(tuple(serverIssueKey, resolved, assignee));
  }

  @Test
  public void should_clear_server_issue_details_if_disappeared() {
    var serverIssueKey = "dummy serverIssueKey";
    var resolved = true;
    var assignee = "dummy assignee";
    var base = builder().ruleKey("dummy ruleKey").severity("CRITICAL").rawSeverity("CRITICAL");

    var start = System.currentTimeMillis();

    // First analysis
    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, List.of(base.build())));
    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackServerIssues(file1,
      List.of(base.copy().serverIssueKey(serverIssueKey).resolved(true).assignee(assignee).severity("BLOCKER").build())));

    // Second analysis with no more issue on server side
    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, List.of(base.build())));
    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackServerIssues(file1, List.of()));

    var trackables = cache.getCurrentTrackables(DUMMY_FILE1_PATH);
    assertThat(trackables)
      .extracting("serverIssueKey", "resolved", "assignee", "severity")
      .containsExactly(tuple(null, !resolved, "", "CRITICAL"));
    assertThat(trackables.iterator().next().getCreationDate()).isGreaterThanOrEqualTo(start);
  }

  @Test
  public void should_ignore_server_issues_when_there_are_no_local() {
    String serverIssueKey = "dummy serverIssueKey";
    boolean resolved = true;
    String assignee = "dummy assignee";
    MockTrackableBuilder base = builder().ruleKey("dummy ruleKey").serverIssueKey(serverIssueKey).resolved(resolved).assignee(assignee);

    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackAsNew(file1, List.of()));
    cache.put(DUMMY_FILE1_PATH, tracker.matchAndTrackServerIssues(file1, List.of(base.build())));

    assertThat(cache.getCurrentTrackables(DUMMY_FILE1_PATH)).isEmpty();
  }
}
