/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2021 SonarSource SA
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
  STARTUP("Startup", ServerIssueUpdateStrategy.PER_FILE_ASYNC),
  EDITOR_OPEN("Editor open", ServerIssueUpdateStrategy.PER_FILE_ASYNC),
  MANUAL("Manual trigger", ServerIssueUpdateStrategy.PER_PROJECT_OR_PER_FILE_SYNC),
  MANUAL_CHANGESET("Manual trigger changeset", ServerIssueUpdateStrategy.PER_PROJECT_OR_PER_FILE_SYNC),
  EDITOR_CHANGE("Editor change", ServerIssueUpdateStrategy.NO_UPDATE),
  BINDING_CHANGE("Binding change", ServerIssueUpdateStrategy.PER_FILE_ASYNC),
  STANDALONE_CONFIG_CHANGE("Standalone config change", ServerIssueUpdateStrategy.NO_UPDATE),
  QUICK_FIX("Quick fix", ServerIssueUpdateStrategy.NO_UPDATE);

  /**
   * Magic number to decide if issues should be fetched per file or once for the entire project
   */
  private static final int PER_FILE_THRESHOLD = 10;

  private final String name;

  private enum ServerIssueUpdateStrategy {
    NO_UPDATE,
    PER_PROJECT_OR_PER_FILE_SYNC,
    PER_FILE_ASYNC
  }

  private final ServerIssueUpdateStrategy updateStrategy;

  TriggerType(String name, ServerIssueUpdateStrategy updateStrategy) {
    this.name = name;
    this.updateStrategy = updateStrategy;
  }

  public String getName() {
    return name;
  }

  public boolean shouldUpdateFileIssuesAsync() {
    return updateStrategy == ServerIssueUpdateStrategy.PER_FILE_ASYNC;
  }

  public boolean shouldUpdateFileIssuesSync(int fileCount) {
    return updateStrategy == ServerIssueUpdateStrategy.PER_PROJECT_OR_PER_FILE_SYNC && fileCount < PER_FILE_THRESHOLD;
  }

  public boolean shouldUpdateProjectIssuesSync(int fileCount) {
    return updateStrategy == ServerIssueUpdateStrategy.PER_PROJECT_OR_PER_FILE_SYNC && fileCount >= PER_FILE_THRESHOLD;
  }

  public boolean isOnTheFly() {
    return this != MANUAL && this != MANUAL_CHANGESET;
  }

}
