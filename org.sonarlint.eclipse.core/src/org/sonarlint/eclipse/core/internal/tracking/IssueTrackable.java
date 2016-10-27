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
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;

public class IssueTrackable extends MutableTrackableImpl {

  private final Issue issue;
  private final TextRange textRange;

  public IssueTrackable(Issue issue) {
    this.issue = issue;
    this.textRange = new TextRange(issue.getStartLine(), issue.getStartLineOffset(), issue.getEndLine(), issue.getEndLineOffset());
  }

  @Override
  public Integer getLine() {
    return issue.getStartLine();
  }

  @Override
  public String getMessage() {
    return issue.getMessage();
  }

  @Override
  public Integer getTextRangeHash() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Integer getLineHash() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getRuleKey() {
    return issue.getRuleKey();
  }

  @Override
  public String getSeverity() {
    return issue.getSeverity();
  }

  @Override
  public TextRange getTextRange() {
    return textRange;
  }
}
