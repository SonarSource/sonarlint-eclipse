/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2022 SonarSource SA
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
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.internal.proto.Sonarlint.Issues.Issue;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarsource.sonarlint.core.analysis.api.Flow;
import org.sonarsource.sonarlint.core.analysis.api.QuickFix;
import org.sonarsource.sonarlint.core.commons.IssueSeverity;
import org.sonarsource.sonarlint.core.commons.RuleType;
import org.sonarsource.sonarlint.core.commons.TextRange;

public class ProtobufIssueTrackable implements Trackable {

  private final Issue issue;

  public ProtobufIssueTrackable(Issue issue) {
    this.issue = issue;
  }

  @Nullable
  @Override
  public Integer getLine() {
    return issue.getLine() != 0 ? issue.getLine() : null;
  }

  @Nullable
  @Override
  public Long getMarkerId() {
    return issue.getMarkerId() == 0 ? null : issue.getMarkerId();
  }

  @Override
  public void setMarkerId(Long id) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getMessage() {
    return issue.getMessage();
  }

  @Nullable
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

  @Nullable
  @Override
  public String getServerIssueKey() {
    return !StringUtils.isEmpty(issue.getServerIssueKey()) ? issue.getServerIssueKey() : null;
  }

  @Nullable
  @Override
  public Long getCreationDate() {
    return issue.getCreationDate() != 0 ? issue.getCreationDate() : null;
  }

  @Override
  public boolean isResolved() {
    return issue.getResolved();
  }

  @Nullable
  @Override
  public IssueSeverity getSeverity() {
    if (!StringUtils.isEmpty(issue.getSeverity())) {
      return IssueSeverity.valueOf(issue.getSeverity());
    }
    return null;
  }

  @Override
  public IssueSeverity getRawSeverity() {
    throw new UnsupportedOperationException();
  }

  @Override
  public RuleType getType() {
    return RuleType.valueOf(issue.getType());
  }

  @Override
  public RuleType getRawType() {
    throw new UnsupportedOperationException();
  }

  @Override
  public TextRange getTextRange() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Flow> getFlows() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<QuickFix> getQuickFix() {
    throw new UnsupportedOperationException();
  }
}
