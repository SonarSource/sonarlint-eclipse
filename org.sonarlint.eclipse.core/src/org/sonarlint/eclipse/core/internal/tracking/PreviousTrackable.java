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
 * Combine a new Trackable ("raw") with a previous state ("base")
 */
public class PreviousTrackable extends WrappedTrackable {

  @Nullable
  private final String serverIssueKey;
  @Nullable
  private final Long creationDate;
  private final boolean resolved;
  @Nullable
  private final IssueSeverity severity;
  private final RuleType type;
  @Nullable
  private Long markerId;

  public PreviousTrackable(Trackable base, Trackable raw) {
    super(raw);

    // Warning: do not store a reference to base, as it might never get garbage collected
    this.serverIssueKey = base.getServerIssueKey();
    this.creationDate = base.getCreationDate();
    this.markerId = base.getMarkerId();
    if (base.getServerIssueKey() != null) {
      this.resolved = base.isResolved();
      this.severity = base.getSeverity();
      this.type = base.getType();
    } else {
      this.resolved = false;
      this.severity = raw.getRawSeverity();
      this.type = raw.getRawType();
    }
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
  public Long getMarkerId() {
    return markerId;
  }

  @Override
  public void setMarkerId(Long markerId) {
    this.markerId = markerId;
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
