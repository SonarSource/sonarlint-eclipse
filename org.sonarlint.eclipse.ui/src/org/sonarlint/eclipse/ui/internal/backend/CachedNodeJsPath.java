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
package org.sonarlint.eclipse.ui.internal.backend;

import java.nio.file.Path;
import java.util.Objects;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.engine.AnalysisRequirementNotifications;
import org.sonarlint.eclipse.ui.internal.binding.actions.AnalysisJobsScheduler;

/**
 * Cache the detected nodejs path, waiting for the backend to control analysis engines
 */
public enum CachedNodeJsPath {

  INSTANCE;

  public static CachedNodeJsPath get() {
    return INSTANCE;
  }

  private boolean initialized;
  private @Nullable String version;
  private @Nullable Path nodeJsPath;

  public synchronized void didChangeNodeJs(@Nullable Path nodeJsPath, @Nullable String version) {
    if (!initialized) {
      this.nodeJsPath = nodeJsPath;
      this.version = version;
      this.initialized = true;
    } else {
      if (!Objects.equals(nodeJsPath, this.nodeJsPath)) {
        // Node.js path is passed at engine startup, so we have to restart them all to ensure the new value is taken
        // into account for the next analysis.
        SonarLintCorePlugin.getInstance().getDefaultSonarLintClientFacade().stop();
        SonarLintCorePlugin.getConnectionManager().getConnections().forEach(f -> f.stop());
        AnalysisRequirementNotifications.resetCachedMessages();
        AnalysisJobsScheduler.scheduleAnalysisOfOpenFiles(null, TriggerType.NODEJS_CONFIG_CHANGE);
      }
      this.nodeJsPath = nodeJsPath;
      this.version = version;
    }
  }

}
