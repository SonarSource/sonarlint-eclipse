package org.sonarlint.eclipse.core.internal.tracking;

import org.sonarlint.eclipse.core.internal.markers.TextRange;
import org.sonarlint.eclipse.core.internal.proto.Sonarlint.Issues.Issue;

public class ProtobufIssueTrackable implements Trackable {

  private final Issue issue;

  public ProtobufIssueTrackable(Issue issue) {
    this.issue = issue;
  }

  @Override
  public Integer getLine() {
    return issue.getLine();
  }

  @Override
  public String getMessage() {
    return issue.getMessage();
  }

  @Override
  public Integer getTextRangeHash() {
    return null;
  }

  @Override
  public Integer getLineHash() {
    return issue.getChecksum();
  }

  @Override
  public String getRuleKey() {
    return issue.getRuleKey();
  }

  @Override
  public String getServerIssueKey() {
    return issue.getServerIssueKey();
  }

  @Override
  public Long getCreationDate() {
    return issue.getCreationDate();
  }

  @Override
  public boolean isResolved() {
    return issue.getResolved();
  }

  @Override
  public String getAssignee() {
    return issue.getAssignee();
  }

  @Override
  public String getSeverity() {
    return "";
  }

  @Override
  public TextRange getTextRange() {
    return null;
  }
}
