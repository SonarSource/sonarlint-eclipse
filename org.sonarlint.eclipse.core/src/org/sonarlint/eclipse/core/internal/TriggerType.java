/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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
  ANALYSIS_READY("Analysis Ready", ServerIssueUpdateStrategy.UPDATE, ServerMatchingStrategy.ASYNC),
  EDITOR_OPEN("Editor open", ServerIssueUpdateStrategy.UPDATE, ServerMatchingStrategy.ASYNC),
  MANUAL("Manual trigger", ServerIssueUpdateStrategy.UPDATE, ServerMatchingStrategy.SYNC),
  MANUAL_CHANGESET("Manual trigger changeset", ServerIssueUpdateStrategy.UPDATE, ServerMatchingStrategy.SYNC),
  EDITOR_CHANGE("Editor change", ServerIssueUpdateStrategy.NO_UPDATE, ServerMatchingStrategy.ASYNC),
  BINDING_CHANGE("Binding change", ServerIssueUpdateStrategy.UPDATE, ServerMatchingStrategy.ASYNC),
  STANDALONE_CONFIG_CHANGE("Standalone config change", ServerIssueUpdateStrategy.NO_UPDATE, ServerMatchingStrategy.ASYNC),
  NODEJS_CONFIG_CHANGE("Node.js config change", ServerIssueUpdateStrategy.NO_UPDATE, ServerMatchingStrategy.ASYNC),
  QUICK_FIX("Quick fix", ServerIssueUpdateStrategy.NO_UPDATE, ServerMatchingStrategy.ASYNC),
  AFTER_RESOLVE("After resolve", ServerIssueUpdateStrategy.NO_UPDATE, ServerMatchingStrategy.ASYNC),
  SERVER_EVENT("Server Event", ServerIssueUpdateStrategy.NO_UPDATE, ServerMatchingStrategy.ASYNC);

  private final String name;

  /**
   * @deprecated this is only used by old SonarQube versions (and SonarCloud). Can be removed when we get issue updates through SSE in all cases (starting from SQ 9.6).
   */
  private enum ServerIssueUpdateStrategy {
    NO_UPDATE,
    UPDATE
  }

  private enum ServerMatchingStrategy {
    /**
     * Wait for server issue matching before creating/updating markers
     */
    SYNC,
    /**
     * Create/update markers as soon as possible after analysis, and update later after server issue matching
     */
    ASYNC
  }

  private final ServerIssueUpdateStrategy updateStrategy;
  private final ServerMatchingStrategy matchingStrategy;

  TriggerType(String name, ServerIssueUpdateStrategy updateStrategy, ServerMatchingStrategy matchingStrategy) {
    this.name = name;
    this.updateStrategy = updateStrategy;
    this.matchingStrategy = matchingStrategy;
  }

  public String getName() {
    return name;
  }

  public boolean shouldUpdate() {
    return updateStrategy == ServerIssueUpdateStrategy.UPDATE;
  }

  public boolean shouldMatchAsync() {
    return matchingStrategy == ServerMatchingStrategy.ASYNC;
  }

  public boolean isOnTheFly() {
    return this != MANUAL && this != MANUAL_CHANGESET;
  }

}
