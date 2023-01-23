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

/**
 * Combine a server Trackable ("serverIssue") with existing issue ("currentIssue")
 */
public class CombinedTrackable extends WrappedTrackable {

  @Nullable
  private final String serverIssueKey;
  @Nullable
  private final Long creationDate;
  private final boolean resolved;
  private final @Nullable IssueSeverity severity;
  private final RuleType type;

  public CombinedTrackable(Trackable serverIssue, Trackable currentIssue) {
    super(currentIssue);

    // Warning: do not store a reference to serverIssue, as it might never get garbage collected
    this.serverIssueKey = serverIssue.getServerIssueKey();
    this.creationDate = serverIssue.getCreationDate();
    this.resolved = serverIssue.isResolved();
    this.severity = serverIssue.getSeverity() != null ? serverIssue.getSeverity() : currentIssue.getRawSeverity();
    this.type = serverIssue.getType();
  }

  @Override
  public String getServerIssueKey() {
    return serverIssueKey;
  }

  @Override
  public Long getCreationDate() {
    return creationDate;
  }

  @Override
  public boolean isResolved() {
    return resolved;
  }

  @Override
  public IssueSeverity getSeverity() {
    return severity;
  }

  @Override
  public RuleType getType() {
    return type;
  }
}
