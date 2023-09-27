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
import org.sonarlint.eclipse.core.internal.tracking.matching.MatchableIssue;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;

import static org.sonarlint.eclipse.core.internal.tracking.DigestUtils.digest;

public class RawIssueTrackable implements MatchableIssue {

  private final Issue issue;
  @Nullable
  private final String textRangeHash;
  @Nullable
  private final String lineHash;
  @Nullable
  private Long markerId;

  /**
   * File-level or project-level issues (no ranges)
   */
  public RawIssueTrackable(Issue issue) {
    this(issue, null, null);
  }

  /**
   * Issue with a range location
   */
  public RawIssueTrackable(Issue issue, @Nullable String textRangeContent, @Nullable String lineContent) {
    this.issue = issue;
    this.textRangeHash = textRangeContent != null ? checksum(textRangeContent) : null;
    this.lineHash = lineContent != null ? checksum(lineContent) : null;
  }

  private static String checksum(String content) {
    return digest(content);
  }

  @Nullable
  @Override
  public Integer getLine() {
    return issue.getStartLine();
  }

  @Nullable
  @Override
  public String getMessage() {
    return issue.getMessage();
  }

  @Override
  public String getTextRangeHash() {
    return textRangeHash;
  }

  @Override
  public String getRuleKey() {
    return issue.getRuleKey();
  }

  public Issue getIssueFromAnalysis() {
    return issue;
  }

  public String getLineHash() {
    return lineHash;
  }

}
