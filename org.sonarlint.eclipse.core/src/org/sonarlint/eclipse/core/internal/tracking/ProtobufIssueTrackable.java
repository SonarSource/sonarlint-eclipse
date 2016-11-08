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

import org.sonarlint.eclipse.core.internal.markers.TextRange;
import org.sonarlint.eclipse.core.internal.proto.Sonarlint.Issues.Issue;

public class ProtobufIssueTrackable implements Trackable {

  private final Issue issue;

  public ProtobufIssueTrackable(Issue issue) {
    this.issue = issue;
  }

  @Override
  public Integer getLine() {
    return issue.getLine() != 0 ? issue.getLine() : null;
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
  public String getRuleName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getServerIssueKey() {
    return issue.getServerIssueKey() != null ? issue.getServerIssueKey() : null;
  }

  @Override
  public Long getCreationDate() {
    return issue.getCreationDate() != 0 ? issue.getCreationDate() : null;
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
    throw new UnsupportedOperationException();
  }

  @Override
  public TextRange getTextRange() {
    throw new UnsupportedOperationException();
  }
}
