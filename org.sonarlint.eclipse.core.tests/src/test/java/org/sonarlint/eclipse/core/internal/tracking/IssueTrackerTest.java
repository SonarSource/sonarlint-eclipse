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

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.sonarlint.eclipse.core.internal.markers.TextRange;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.connected.ServerIssue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IssueTrackerTest {

  private final IssueTrackerCache cache = new InMemoryIssueTrackerCache();
  private final TrackingChangeSubmitter submitter = mock(TrackingChangeSubmitter.class);
  private final IssueTracker tracker = new IssueTracker(cache, submitter);

  private final String file1 = "dummyFile1";

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
        .assignee(assignee);
    }

    Trackable build() {
      Trackable mock = mock(Trackable.class);
      when(mock.getLine()).thenReturn(line);
      when(mock.getTextRangeHash()).thenReturn(textRangeHash);
      when(mock.getLineHash()).thenReturn(lineHash);
      when(mock.getRuleKey()).thenReturn(ruleKey);
      when(mock.getMessage()).thenReturn(message);
      when(mock.getCreationDate()).thenReturn(creationDate);
      when(mock.getServerIssueKey()).thenReturn(serverIssueKey);
      when(mock.isResolved()).thenReturn(resolved);
      when(mock.getAssignee()).thenReturn(assignee);

      // set unique values for nullable fields used by the matchers in Tracker
      if (line == null) {
        when(mock.getLine()).thenReturn(counter++);
      }
      if (lineHash == null) {
        when(mock.getLineHash()).thenReturn(counter++);
      }
      if (textRangeHash == null) {
        when(mock.getTextRangeHash()).thenReturn(counter++);
      }

      return mock;
    }
  }

  private static MockTrackableBuilder builder() {
    return new MockTrackableBuilder();
  }

  private Issue mockIssue() {
    Issue issue = mock(Issue.class);
    when(issue.getRuleKey()).thenReturn("dummy ruleKey");
    when(issue.getMessage()).thenReturn("dummy message");
    return issue;
  }

  @Before
  public void setUp() {
    cache.clear();
  }

  @Test
  public void should_track_first_trackables_exactly() {
    Collection<Trackable> trackables = Arrays.asList(mock(Trackable.class), mock(Trackable.class));
    tracker.matchAndTrackAsNew(file1, trackables);
    assertThat(cache.getCurrentTrackables(file1)).isEqualTo(trackables);
  }

  @Test
  public void should_preserve_known_standalone_trackables_with_null_date() {
    Collection<Trackable> trackables = Arrays.asList(trackable1, trackable2);
    tracker.matchAndTrackAsNew(file1, trackables);
    tracker.matchAndTrackAsNew(file1, trackables);

    Collection<Trackable> next = cache.getCurrentTrackables(file1);
    assertThat(next).extracting(t -> t.getLine()).containsExactlyInAnyOrder(trackable1.getLine(), trackable2.getLine());
    assertThat(next).extracting(t -> t.getCreationDate()).containsExactlyInAnyOrder(null, null);
  }

  @Test
  public void should_add_timestamp_for_leaked_trackables() {
    long start = System.currentTimeMillis();

    tracker.matchAndTrackAsNew(file1, Collections.singletonList(trackable1));
    tracker.matchAndTrackAsNew(file1, Arrays.asList(trackable1, trackable2));

    Collection<Trackable> next = cache.getCurrentTrackables(file1);
    assertThat(next).extracting(t -> t.getLine()).contains(trackable1.getLine(), trackable2.getLine());

    assertThat(next).extracting(t -> t.getCreationDate()).containsOnlyOnce((Long) null);

    Trackable leaked = next.stream().filter(t -> t.getCreationDate() != null).findFirst().get();
    assertThat(leaked.getCreationDate()).isGreaterThanOrEqualTo(start);
    assertThat(leaked.getLine()).isEqualTo(trackable2.getLine());
  }

  @Test
  public void should_drop_disappeared_issues() {
    tracker.matchAndTrackAsNew(file1, Arrays.asList(trackable1, trackable2));
    tracker.matchAndTrackAsNew(file1, Collections.singletonList(trackable1));

    Collection<Trackable> next = cache.getCurrentTrackables(file1);
    assertThat(next).extracting(t -> t.getLine()).containsExactly(trackable1.getLine());
  }

  @Test
  public void should_not_match_trackables_with_different_rule_key() {
    String ruleKey = "dummy ruleKey";
    MockTrackableBuilder base = builder()
      .line(7)
      .message("dummy message")
      .textRangeHash(11)
      .lineHash(13)
      .ruleKey(ruleKey)
      .serverIssueKey("dummy serverIssueKey")
      .creationDate(17L)
      .assignee("dummy assignee");

    tracker.matchAndTrackAsNew(file1, Collections.singletonList(base.build()));
    tracker.matchAndTrackAsNew(file1, Collections.singletonList(base.ruleKey(ruleKey + "x").build()));
  }

  @Test
  public void should_treat_new_issues_as_leak_when_old_issues_disappeared() {
    long start = System.currentTimeMillis();

    tracker.matchAndTrackAsNew(file1, Collections.singletonList(trackable1));
    tracker.matchAndTrackAsNew(file1, Collections.singletonList(trackable2));

    Collection<Trackable> next = cache.getCurrentTrackables(file1);
    assertThat(next).extracting(t -> t.getLine()).containsExactly(trackable2.getLine());

    Trackable leaked = next.stream().filter(t -> t.getCreationDate() != null).findFirst().get();
    assertThat(leaked.getCreationDate()).isGreaterThanOrEqualTo(start);
  }

  @Test
  public void should_match_by_line_and_text_range_hash() {
    MockTrackableBuilder base = builder().ruleKey("dummy ruleKey");
    int line = 7;
    int textRangeHash = 11;
    // note: (ab)using the assignee field to uniquely identify the trackable
    String id = "dummy id";
    tracker.matchAndTrackAsNew(file1, Collections.singletonList(base.copy().line(line).textRangeHash(textRangeHash).assignee(id).build()));

    Trackable differentLine = base.line(line + 1).textRangeHash(textRangeHash).build();
    Trackable differentTextRangeHash = base.line(line).textRangeHash(textRangeHash + 1).build();
    Trackable differentBoth = base.line(line + 1).textRangeHash(textRangeHash + 1).build();
    Trackable same = base.line(line).textRangeHash(textRangeHash).build();
    tracker.matchAndTrackAsNew(file1, Arrays.asList(differentLine, differentTextRangeHash, differentBoth, same));

    Collection<Trackable> current = cache.getCurrentTrackables(file1);
    assertThat(current).hasSize(4);
    assertThat(current).extracting("assignee").containsOnlyOnce(id);
    assertThat(current).extracting("line", "textRangeHash", "assignee").containsOnlyOnce(tuple(line, textRangeHash, id));
  }

  @Test
  public void should_match_by_line_and_message() {
    MockTrackableBuilder base = builder().ruleKey("dummy ruleKey");
    int line = 7;
    String message = "should make this condition not always false";
    // note: (ab)using the assignee field to uniquely identify the trackable
    String id = "dummy id";
    int c = 1;
    tracker.matchAndTrackAsNew(file1, Collections.singletonList(base.copy().line(line).message(message).assignee(id).textRangeHash(c++).build()));

    Trackable differentLine = base.line(line + 1).message(message).textRangeHash(c++).build();
    Trackable differentMessage = base.line(line).message(message + "x").textRangeHash(c++).build();
    Trackable differentBoth = base.line(line + 1).message(message + "x").textRangeHash(c++).build();
    Trackable same = base.line(line).message(message).textRangeHash(c++).build();
    tracker.matchAndTrackAsNew(file1, Arrays.asList(differentLine, differentMessage, differentBoth, same));

    Collection<Trackable> current = cache.getCurrentTrackables(file1);
    assertThat(current).hasSize(4);
    assertThat(current).extracting("assignee").containsOnlyOnce(id);
    assertThat(current).extracting("line", "message", "assignee").containsOnlyOnce(tuple(line, message, id));
  }

  @Test
  public void should_match_by_text_range_hash() {
    MockTrackableBuilder base = builder().ruleKey("dummy ruleKey").textRangeHash(11);
    // note: (ab)using the assignee field to uniquely identify the trackable
    String id = "dummy id";
    int newLine = 7;

    tracker.matchAndTrackAsNew(file1, Collections.singletonList(base.copy().line(newLine + 3).assignee(id).build()));
    tracker.matchAndTrackAsNew(file1, Collections.singletonList(base.line(newLine).build()));

    assertThat(cache.getCurrentTrackables(file1)).extracting("line", "assignee").containsExactly(tuple(newLine, id));
  }

  @Test
  public void should_match_by_line_hash() {
    MockTrackableBuilder base = builder().ruleKey("dummy ruleKey").lineHash(11);
    // note: (ab)using the assignee field to uniquely identify the trackable
    String id = "dummy id";
    int newLine = 7;

    tracker.matchAndTrackAsNew(file1, Collections.singletonList(base.copy().line(newLine + 3).assignee(id).build()));
    tracker.matchAndTrackAsNew(file1, Collections.singletonList(base.line(newLine).build()));

    assertThat(cache.getCurrentTrackables(file1)).extracting("line", "assignee").containsExactly(tuple(newLine, id));
  }

  @Test
  public void should_match_local_issues_by_line_hash() {
    String lineContent = "dummy content";
    int newLine = 7;

    Issue issue = mockIssue();
    when(issue.getStartLine()).thenReturn(newLine + 3);

    Issue movedIssue = mockIssue();
    when(movedIssue.getStartLine()).thenReturn(newLine);

    Trackable trackable = new IssueTrackable(issue, mock(TextRange.class), null, lineContent);
    Trackable movedTrackable = new IssueTrackable(movedIssue, mock(TextRange.class), null, lineContent);
    Trackable nonMatchingTrackable = new IssueTrackable(mockIssue(), mock(TextRange.class), null, lineContent + "x");

    tracker.matchAndTrackAsNew(file1, Collections.singletonList(trackable));
    tracker.matchAndTrackAsNew(file1, Arrays.asList(movedTrackable, nonMatchingTrackable));

    assertThat(movedTrackable.getLineHash()).isEqualTo(trackable.getLineHash());
    assertThat(movedTrackable.getLineHash()).isNotEqualTo(nonMatchingTrackable.getLineHash());

    Collection<Trackable> next = cache.getCurrentTrackables(file1);

    // matched trackable has no date
    assertThat(next.stream().filter(t -> t.getCreationDate() == null)).extracting("line", "lineHash").containsOnly(
      tuple(movedTrackable.getLine(), movedTrackable.getLineHash()));

    // unmatched trackable has a date -> it is a leak
    assertThat(next.stream().filter(t -> t.getCreationDate() != null)).extracting("line", "lineHash").containsOnly(
      tuple(nonMatchingTrackable.getLine(), nonMatchingTrackable.getLineHash()));
  }

  @Test
  public void should_match_server_issues_by_line_hash() {
    String ruleKey = "dummy ruleKey";
    String message = "dummy message";
    String lineContent = "dummy content";
    int newLine = 7;
    String serverIssueKey = "dummy serverIssueKey";

    Issue issue = mock(Issue.class);
    when(issue.getRuleKey()).thenReturn(ruleKey);
    when(issue.getMessage()).thenReturn(message);
    when(issue.getStartLine()).thenReturn(newLine);
    Trackable trackable = new IssueTrackable(issue, mock(TextRange.class), null, lineContent);

    ServerIssue serverIssue = mock(ServerIssue.class);
    when(serverIssue.ruleKey()).thenReturn(ruleKey);
    when(serverIssue.message()).thenReturn(message);
    when(serverIssue.checksum()).thenReturn(DigestUtils.digest(lineContent));
    when(serverIssue.line()).thenReturn(newLine + 3);
    when(serverIssue.creationDate()).thenReturn(Instant.now());
    when(serverIssue.key()).thenReturn(serverIssueKey);
    when(serverIssue.resolution()).thenReturn("fixed");
    Trackable movedTrackable = new ServerIssueTrackable(serverIssue);

    Trackable nonMatchingTrackable = new IssueTrackable(mockIssue(), mock(TextRange.class), null, lineContent + "x");

    tracker.matchAndTrackAsNew(file1, Collections.singletonList(trackable));
    tracker.matchAndTrackAsBase(file1, Arrays.asList(movedTrackable, nonMatchingTrackable));

    assertThat(movedTrackable.getLineHash()).isEqualTo(trackable.getLineHash());
    assertThat(movedTrackable.getLineHash()).isNotEqualTo(nonMatchingTrackable.getLineHash());

    assertThat(cache.getCurrentTrackables(file1)).extracting("line", "lineHash", "serverIssueKey", "resolved").containsOnly(
      tuple(newLine, movedTrackable.getLineHash(), movedTrackable.getServerIssueKey(), movedTrackable.isResolved()));
  }

  @Test
  public void should_match_by_server_issue_key() {
    MockTrackableBuilder base = builder().ruleKey("dummy ruleKey").serverIssueKey("dummy server issue key");
    // note: (ab)using the assignee field to uniquely identify the trackable
    String id = "dummy id";
    int newLine = 7;

    tracker.matchAndTrackAsNew(file1, Collections.singletonList(base.copy().line(newLine + 3).assignee(id).build()));
    tracker.matchAndTrackAsNew(file1, Collections.singletonList(base.line(newLine).build()));

    assertThat(cache.getCurrentTrackables(file1)).extracting("line", "assignee").containsExactly(tuple(newLine, id));
  }

  @Test
  public void should_preserve_creation_date() {
    MockTrackableBuilder base = builder().ruleKey("dummy ruleKey").line(7).textRangeHash(11);
    // note: (ab)using the assignee field to uniquely identify the trackable
    String id = "dummy id";
    long creationDate = 123;

    tracker.matchAndTrackAsNew(file1, Collections.singletonList(base.copy().creationDate(creationDate).assignee(id).build()));
    tracker.matchAndTrackAsNew(file1, Collections.singletonList(base.build()));

    assertThat(cache.getCurrentTrackables(file1)).extracting("creationDate", "assignee").containsExactly(tuple(creationDate, id));
  }

  @Test
  public void should_preserve_server_issue_details() {
    MockTrackableBuilder base = builder().ruleKey("dummy ruleKey").line(7).textRangeHash(11);
    // note: (ab)using the assignee field to uniquely identify the trackable
    String id = "dummy id";
    String serverIssueKey = "dummy serverIssueKey";
    boolean resolved = true;

    tracker.matchAndTrackAsNew(file1, Collections.singletonList(base.copy().serverIssueKey(serverIssueKey).resolved(resolved).assignee(id).build()));
    tracker.matchAndTrackAsNew(file1, Collections.singletonList(base.build()));

    assertThat(cache.getCurrentTrackables(file1)).extracting("serverIssueKey", "resolved", "assignee").containsExactly(tuple(serverIssueKey, resolved, id));
  }

  @Test
  public void should_drop_server_issue_reference_if_gone() {
    MockTrackableBuilder base = builder().ruleKey("dummy ruleKey").line(7).textRangeHash(11);
    // note: (ab)using the assignee field to uniquely identify the trackable
    String id = "dummy id";
    String serverIssueKey = "dummy serverIssueKey";
    boolean resolved = true;

    tracker.matchAndTrackAsNew(file1, Collections.singletonList(base.copy().serverIssueKey(serverIssueKey).resolved(resolved).assignee(id).build()));
    tracker.matchAndTrackAsBase(file1, Collections.singletonList(base.build()));

    assertThat(cache.getCurrentTrackables(file1)).extracting("serverIssueKey", "resolved", "assignee").containsExactly(tuple(null, false, ""));
  }

  @Test
  public void should_update_server_issue_details() {
    String serverIssueKey = "dummy serverIssueKey";
    boolean resolved = true;
    String assignee = "dummy assignee";
    MockTrackableBuilder base = builder().ruleKey("dummy ruleKey").serverIssueKey(serverIssueKey).resolved(resolved).assignee(assignee);

    tracker.matchAndTrackAsNew(file1, Collections.singletonList(base.copy().resolved(!resolved).assignee(assignee + "x").build()));
    tracker.matchAndTrackAsBase(file1, Collections.singletonList(base.build()));

    assertThat(cache.getCurrentTrackables(file1)).extracting("serverIssueKey", "resolved", "assignee").containsExactly(tuple(serverIssueKey, resolved, assignee));
  }

  @Test
  public void should_clear_server_issue_details_if_disappeared() {
    String serverIssueKey = "dummy serverIssueKey";
    boolean resolved = true;
    String assignee = "dummy assignee";
    MockTrackableBuilder base = builder().ruleKey("dummy ruleKey").serverIssueKey(serverIssueKey).resolved(resolved).assignee(assignee);

    tracker.matchAndTrackAsNew(file1, Collections.emptyList());
    tracker.matchAndTrackAsNew(file1, Collections.singletonList(base.build()));

    assertThat(cache.getCurrentTrackables(file1)).extracting("serverIssueKey", "resolved", "assignee").containsExactly(tuple(null, !resolved, ""));
  }

  @Test
  public void should_ignore_server_issues_when_there_are_no_local() {
    String serverIssueKey = "dummy serverIssueKey";
    boolean resolved = true;
    String assignee = "dummy assignee";
    MockTrackableBuilder base = builder().ruleKey("dummy ruleKey").serverIssueKey(serverIssueKey).resolved(resolved).assignee(assignee);

    tracker.matchAndTrackAsNew(file1, Collections.emptyList());
    tracker.matchAndTrackAsBase(file1, Collections.singletonList(base.build()));

    assertThat(cache.getCurrentTrackables(file1)).isEmpty();
  }
}
