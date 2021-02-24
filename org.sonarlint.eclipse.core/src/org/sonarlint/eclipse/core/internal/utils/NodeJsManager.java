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
package org.sonarlint.eclipse.core.internal.utils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.AnalysisRequirementNotifications;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarsource.sonarlint.core.NodeJsHelper;
import org.sonarsource.sonarlint.core.client.api.common.Version;

public class NodeJsManager {

  private boolean nodeInit = false;
  private Path nodeJsPath = null;
  private Version nodeJsVersion = null;

  /**
   * Reload path from global preferences.
   * Should be called when preferences are changed.
   */
  public void reload() {
    if (!Objects.equals(Paths.get(SonarLintGlobalConfiguration.getNodejsPath()), nodeJsPath)) {
      clear();
      // Node.js path is passed at engine startup, so we have to restart them all to ensure the new value is taken into account
      SonarLintCorePlugin.getInstance().getDefaultSonarLintClientFacade().stop();
      SonarLintCorePlugin.getServersManager().getServers().forEach(f -> ((ConnectedEngineFacade) f).stop());
      AnalysisRequirementNotifications.resetCachedMessages();
    }
  }

  private synchronized void clear() {
    this.nodeInit = false;
    this.nodeJsPath = null;
    this.nodeJsVersion = null;
  }

  private synchronized void initNodeIfNeeded() {
    if (!nodeInit) {
      NodeJsHelper helper = new NodeJsHelper();
      helper.detect(getNodeJsPathFromConfig());
      this.nodeInit = true;
      this.nodeJsPath = helper.getNodeJsPath();
      this.nodeJsVersion = helper.getNodeJsVersion();
    }
  }

  @Nullable
  public Path getNodeJsPath() {
    initNodeIfNeeded();
    return nodeJsPath;
  }

  @Nullable
  public Version getNodeJsVersion() {
    initNodeIfNeeded();
    return nodeJsVersion;
  }

  @Nullable
  private static Path getNodeJsPathFromConfig() {
    final String nodejsPathStr = SonarLintGlobalConfiguration.getNodejsPath();
    if (StringUtils.isNotBlank(nodejsPathStr)) {
      try {
        return Paths.get(nodejsPathStr);
      } catch (Exception e) {
        throw new IllegalStateException("Invalid Node.js path", e);
      }
    }
    return null;
  }

}
