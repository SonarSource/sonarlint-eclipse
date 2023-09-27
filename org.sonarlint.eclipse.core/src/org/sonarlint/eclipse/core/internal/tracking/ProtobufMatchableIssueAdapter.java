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

import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.internal.proto.Sonarlint.Issues.Issue;
import org.sonarlint.eclipse.core.internal.tracking.matching.MatchableIssue;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;

public class ProtobufMatchableIssueAdapter implements MatchableIssue {
  private final Issue issue;

  public ProtobufMatchableIssueAdapter(Issue issue) {
    this.issue = issue;
  }

  @Nullable
  @Override
  public Integer getLine() {
    return issue.getLine() != 0 ? issue.getLine() : null;
  }

  @Override
  public String getMessage() {
    return issue.getMessage();
  }

  @Nullable
  @Override
  public String getTextRangeHash() {
    return StringUtils.trimToNull(issue.getTextRangeDigest());
  }

  @Override
  public String getRuleKey() {
    return issue.getRuleKey();
  }

  public Issue getProtobufIssue() {
    return issue;
  }

}
