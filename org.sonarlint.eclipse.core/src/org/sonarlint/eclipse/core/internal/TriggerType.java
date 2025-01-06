/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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
  ANALYSIS_READY("Analysis Ready", ServerIssueFetchStrategy.FETCH),
  EDITOR_OPEN("Editor open", ServerIssueFetchStrategy.FETCH),
  MANUAL("Manual trigger", ServerIssueFetchStrategy.FETCH),
  MANUAL_CHANGESET("Manual trigger changeset", ServerIssueFetchStrategy.FETCH),
  EDITOR_CHANGE("Editor change", ServerIssueFetchStrategy.DONT_FETCH),
  BINDING_CHANGE("Binding change", ServerIssueFetchStrategy.FETCH),
  STANDALONE_CONFIG_CHANGE("Standalone config change", ServerIssueFetchStrategy.DONT_FETCH),
  QUICK_FIX("Quick fix", ServerIssueFetchStrategy.DONT_FETCH),
  AFTER_RESOLVE("After resolve", ServerIssueFetchStrategy.DONT_FETCH);

  /** For the analysis out of process this information is required */
  private enum ServerIssueFetchStrategy {
    DONT_FETCH,
    FETCH
  }

  private final String name;
  private final ServerIssueFetchStrategy fetchStrategy;

  TriggerType(String name, ServerIssueFetchStrategy fetchStrategy) {
    this.name = name;
    this.fetchStrategy = fetchStrategy;
  }

  public String getName() {
    return name;
  }

  public boolean shouldFetch() {
    return fetchStrategy == ServerIssueFetchStrategy.FETCH;
  }

  public boolean isOnTheFly() {
    return this != MANUAL && this != MANUAL_CHANGESET;
  }
}
