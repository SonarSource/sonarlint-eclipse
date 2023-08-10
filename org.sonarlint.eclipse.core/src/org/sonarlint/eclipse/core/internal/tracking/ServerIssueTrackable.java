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
import org.sonarsource.sonarlint.core.commons.IssueSeverity;
import org.sonarsource.sonarlint.core.commons.RuleType;
import org.sonarsource.sonarlint.core.commons.TextRange;
import org.sonarsource.sonarlint.core.serverconnection.issues.LineLevelServerIssue;
import org.sonarsource.sonarlint.core.serverconnection.issues.RangeLevelServerIssue;
import org.sonarsource.sonarlint.core.serverconnection.issues.ServerIssue;

public class ServerIssueTrackable implements Trackable {
  private final ServerIssue serverIssue;

  public ServerIssueTrackable(ServerIssue serverIssue) {
    this.serverIssue = serverIssue;
  }

  @Nullable
  @Override
  public Integer getLine() {
    if (serverIssue instanceof LineLevelServerIssue) {
      return ((LineLevelServerIssue) serverIssue).getLine();
    }
    if (serverIssue instanceof RangeLevelServerIssue) {
      return ((RangeLevelServerIssue) serverIssue).getTextRange().getStartLine();
    }
    return null;
  }

  @Nullable
  @Override
  public String getMessage() {
    return serverIssue.getMessage();
  }

  @Nullable
  @Override
  public Integer getTextRangeHash() {
    if (serverIssue instanceof RangeLevelServerIssue) {
      return ((RangeLevelServerIssue) serverIssue).getTextRange().getHash().hashCode();
    }
    return null;
  }

  @Nullable
  @Override
  public Integer getLineHash() {
    if (serverIssue instanceof LineLevelServerIssue) {
      return ((LineLevelServerIssue) serverIssue).getLineHash().hashCode();
    }
    return null;
  }

  @Override
  public String getRuleKey() {
    return serverIssue.getRuleKey();
  }

  @Override
  public Long getCreationDate() {
    return serverIssue.getCreationDate().toEpochMilli();
  }

  @Override
  public String getServerIssueKey() {
    return serverIssue.getKey();
  }

  @Override
  public boolean isResolved() {
    return serverIssue.isResolved();
  }

  @Nullable
  @Override
  public IssueSeverity getSeverity() {
    return serverIssue.getUserSeverity();
  }

  @Override
  public RuleType getType() {
    return serverIssue.getType();
  }

  @Nullable
  @Override
  public TextRange getTextRange() {
    if (serverIssue instanceof RangeLevelServerIssue) {
      return ((RangeLevelServerIssue) serverIssue).getTextRange();
    }
    return null;
  }
}
