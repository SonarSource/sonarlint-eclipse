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
package org.sonarlint.eclipse.core.internal.utils;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.AnalysisRequirementNotifications;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarsource.sonarlint.core.NodeJsHelper;
import org.sonarsource.sonarlint.core.commons.Version;

/**
 *  Disclaimer on macOS:
 *  We try to detect a Node.js installation via the macOS built-in "path_helper" (resides in: /usr/libexec) utility.
 *  This causes an issue with automatically detecting a Node.js installation when it is added to $PATH from the user
 *  shell configuration (e.g. .bashrc for Bash, .zprofile for Zsh) and due to running the command from within Eclipse,
 *  the underlying shell spawned via {@link NodeJsHelper#computePathEnvForMacOs} has an incomplete $PATH variable.
 *  Therefore integration tests running in such an environment will fail as we assume Node.js to be found on $PATH!
 *
 *  For further information, see <a href="https://gist.github.com/Linerre/f11ad4a6a934dcf01ee8415c9457e7b2">here</a>!
 */
public class NodeJsManager {
  private boolean nodeInit;
  @Nullable
  private Path nodeJsPath;
  @Nullable
  private Version nodeJsVersion;

  /**
   * Reload path from global preferences.
   * Should be called when preferences are changed.
   */
  public void reload() {
    if (!Objects.equals(Paths.get(SonarLintGlobalConfiguration.getNodejsPath()), nodeJsPath)) {
      clear();
      // Node.js path is passed at engine startup, so we have to restart them all to ensure the new value is taken
      // into account for the next analysis.
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
      var helper = new NodeJsHelper();
      helper.detect(getNodeJsPathFromConfig());
      this.nodeInit = true;
      this.nodeJsPath = helper.getNodeJsPath();
      this.nodeJsVersion = helper.getNodeJsVersion();

      if (this.nodeJsPath == null) {
        SonarLintLogger.get().info(
          "Node.js could not be automatically detected, has to be configured manually in the SonarLint preferences!");

        var isMac = isMac();
        if (isMac.isEmpty() || isMac.get().booleanValue()) {
          // In case of macOS or could not be found, just add the warning for the user and us if we have to provide
          // support on that matter at some point.
          SonarLintLogger.get().info(
            "Automatic detection does not work on macOS when added to PATH from user shell configuration (e.g. Bash)");
        }
      }
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
    final var nodejsPathStr = SonarLintGlobalConfiguration.getNodejsPath();
    if (StringUtils.isNotBlank(nodejsPathStr)) {
      try {
        return Paths.get(nodejsPathStr);
      } catch (InvalidPathException e) {
        throw new IllegalStateException("Invalid Node.js path", e);
      }
    }
    return null;
  }

  public static Optional<Boolean> isMac() {
    try {
      var os = System.getProperty("os.name");

      // To be or not to be macOS
      if (os == null) {
        return Optional.empty();
      }
      return Optional.of(Boolean.valueOf(os.toLowerCase().contains("mac")));
    } catch (Exception ignored) {
      return Optional.of(Boolean.valueOf(false));
    }
  }
}
