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
package org.sonarlint.eclipse.core.internal;

public enum TriggerType {
  STARTUP("Startup", ServerIssueUpdateStrategy.UPDATE),
  EDITOR_OPEN("Editor open", ServerIssueUpdateStrategy.UPDATE),
  MANUAL("Manual trigger", ServerIssueUpdateStrategy.UPDATE),
  MANUAL_CHANGESET("Manual trigger changeset", ServerIssueUpdateStrategy.UPDATE),
  EDITOR_CHANGE("Editor change", ServerIssueUpdateStrategy.NO_UPDATE),
  BINDING_CHANGE("Binding change", ServerIssueUpdateStrategy.UPDATE),
  STANDALONE_CONFIG_CHANGE("Standalone config change", ServerIssueUpdateStrategy.NO_UPDATE),
  QUICK_FIX("Quick fix", ServerIssueUpdateStrategy.NO_UPDATE),
  AFTER_RESOLVE("After resolve", ServerIssueUpdateStrategy.NO_UPDATE),
  SERVER_EVENT("Server Event", ServerIssueUpdateStrategy.NO_UPDATE);

  private final String name;

  private enum ServerIssueUpdateStrategy {
    NO_UPDATE,
    UPDATE
  }

  private final ServerIssueUpdateStrategy updateStrategy;

  TriggerType(String name, ServerIssueUpdateStrategy updateStrategy) {
    this.name = name;
    this.updateStrategy = updateStrategy;
  }

  public String getName() {
    return name;
  }

  public boolean shouldUpdate() {
    return updateStrategy == ServerIssueUpdateStrategy.UPDATE;
  }

  public boolean isOnTheFly() {
    return this != MANUAL && this != MANUAL_CHANGESET;
  }

}
