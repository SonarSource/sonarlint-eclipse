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
 * A trackable that used to match a server issue but it no longer does.
 */
public class DisconnectedTrackable extends WrappedTrackable {

  private final IssueSeverity severity;
  private final RuleType type;

  public DisconnectedTrackable(Trackable trackable) {
    super(trackable);
    this.severity = trackable.getRawSeverity();
    this.type = trackable.getRawType();
  }

  @Nullable
  @Override
  public String getServerIssueKey() {
    return null;
  }

  @Override
  public boolean isResolved() {
    return false;
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
