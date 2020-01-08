/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2020 SonarSource SA
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
import org.sonarlint.eclipse.core.internal.markers.TextRange;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue.Flow;
import org.sonarsource.sonarlint.core.client.api.connected.ServerIssue;

public class ServerIssueTrackable implements Trackable {

  private final ServerIssue serverIssue;

  public ServerIssueTrackable(ServerIssue serverIssue) {
    this.serverIssue = serverIssue;
  }

  @Override
  public Long getMarkerId() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setMarkerId(Long id) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Integer getLine() {
    return serverIssue.line();
  }

  @Override
  public String getMessage() {
    return serverIssue.message();
  }

  @Override
  public Integer getTextRangeHash() {
    // note: not available from server API
    return null;
  }

  @Override
  public Integer getLineHash() {
    return serverIssue.checksum().hashCode();
  }

  @Override
  public String getRuleKey() {
    return serverIssue.ruleKey();
  }

  @Override
  public String getRuleName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Long getCreationDate() {
    return serverIssue.creationDate().toEpochMilli();
  }

  @Override
  public String getServerIssueKey() {
    return serverIssue.key();
  }

  @Override
  public boolean isResolved() {
    return !serverIssue.resolution().isEmpty();
  }

  @Override
  public String getAssignee() {
    return serverIssue.assigneeLogin();
  }

  @Override
  public String getSeverity() {
    return serverIssue.severity();
  }

  @Override
  public String getRawSeverity() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getType() {
    // FIXME We are not able to get server side issue type
    throw new UnsupportedOperationException();
  }

  @Override
  public String getRawType() {
    throw new UnsupportedOperationException();
  }

  @Override
  public TextRange getTextRange() {
    return TextRange.get(serverIssue.line());
  }

  @Override
  public List<Flow> getFlows() {
    throw new UnsupportedOperationException();
  }
}
