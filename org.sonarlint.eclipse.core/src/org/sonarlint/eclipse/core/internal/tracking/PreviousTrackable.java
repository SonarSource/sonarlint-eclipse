/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2019 SonarSource SA
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

import org.sonarlint.eclipse.core.internal.utils.StringUtils;

/**
 * Combine a new Trackable ("raw") with a previous state ("base")
 */
public class PreviousTrackable extends WrappedTrackable {

  private final String serverIssueKey;
  private final Long creationDate;
  private final boolean resolved;
  private final String assignee;
  private final String severity;
  private final String type;
  private Long markerId;

  public PreviousTrackable(Trackable base, Trackable raw) {
    super(raw);

    // Warning: do not store a reference to base, as it might never get garbage collected
    this.serverIssueKey = base.getServerIssueKey();
    this.creationDate = base.getCreationDate();
    this.resolved = base.isResolved();
    this.assignee = base.getAssignee();
    this.markerId = base.getMarkerId();
    // Migration: severity & type were initially not stored in protobuf file
    this.severity = StringUtils.isBlank(base.getSeverity()) ? raw.getSeverity() : base.getSeverity();
    this.type = StringUtils.isBlank(base.getType()) ? raw.getType() : base.getType();
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
  public String getAssignee() {
    return assignee;
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
  public String getSeverity() {
    return severity;
  }

  @Override
  public String getType() {
    return type;
  }
}
