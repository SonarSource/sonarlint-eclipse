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

public class WrappedTrackable implements Trackable {

  private final Trackable trackable;

  public WrappedTrackable(Trackable trackable) {
    this.trackable = trackable;
  }

  @Override
  public Integer getLine() {
    return trackable.getLine();
  }

  @Override
  public String getMessage() {
    return trackable.getMessage();
  }

  @Override
  public Integer getTextRangeHash() {
    return trackable.getTextRangeHash();
  }

  @Override
  public Integer getLineHash() {
    return trackable.getLineHash();
  }

  @Override
  public String getRuleKey() {
    return trackable.getRuleKey();
  }

  @Override
  public String getRuleName() {
    return trackable.getRuleName();
  }

  @Override
  public String getSeverity() {
    return trackable.getSeverity();
  }

  @Override
  public TextRange getTextRange() {
    return trackable.getTextRange();
  }

  @Override
  public String getServerIssueKey() {
    return trackable.getServerIssueKey();
  }

  @Override
  public Long getCreationDate() {
    return trackable.getCreationDate();
  }

  @Override
  public boolean isResolved() {
    return trackable.isResolved();
  }

  @Override
  public String getAssignee() {
    return trackable.getAssignee();
  }
}
