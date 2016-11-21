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
package org.sonarlint.eclipse.core.internal;

import java.nio.file.Path;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

/**
 * Utility class to centralize access to relevant global paths.
 */
public class StorageManager {

  private StorageManager() {
    // utility class, forbidden constructor
  }

  private static IPath getSonarLintUserHome() {
    return ResourcesPlugin.getWorkspace().getRoot().getLocation().append(".sonarlint");
  }

  public static Path getServerWorkDir(String serverId) {
    return getSonarLintUserHome().append("work").append(serverId).toFile().toPath();
  }

  public static Path getServerStorageRoot() {
    return getSonarLintUserHome().append("storage").toFile().toPath();
  }

  public static Path getIssuesDir(String localModuleKey) {
    return getSonarLintUserHome().append("modules").append(localModuleKey).append("issues").toFile().toPath();
  }
}
