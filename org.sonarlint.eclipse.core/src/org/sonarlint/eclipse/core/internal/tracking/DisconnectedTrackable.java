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

/**
 * A trackable that used to match a server issue but it no longer does.
 */
public class DisconnectedTrackable extends WrappedTrackable {

  private final String severity;
  private final String type;

  public DisconnectedTrackable(Trackable trackable) {
    super(trackable);
    this.severity = trackable.getRawSeverity();
    this.type = trackable.getRawType();
  }

  @Override
  public String getServerIssueKey() {
    return null;
  }

  @Override
  public boolean isResolved() {
    return false;
  }

  @Override
  public String getAssignee() {
    return "";
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
