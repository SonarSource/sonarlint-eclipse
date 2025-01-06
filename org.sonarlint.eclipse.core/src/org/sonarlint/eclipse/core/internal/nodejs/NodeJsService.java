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
package org.sonarlint.eclipse.core.internal.nodejs;

import java.nio.file.Path;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.internal.utils.BundleUtils;

public class NodeJsService {
  /**
   *  This has to be on par with the
   *  - minimal requirement of the embedded SonarJS analyzer (META-INF/MANIFEST.MF -> "NodeJs-Min-Version")
   *  - bundled Node.js version in Eclipse WWD at https://github.com/eclipse-wildwebdeveloper/wildwebdeveloper
   *    -> org.eclipse.wildwebdeveloper.node/META-INF/MANIFEST.MF -> "Bundle-SymbolicName"
   *    -> org.eclipse.wildwebdeveloper.node.win32.x86_64/nodejs-info.properties -> "archiveURL"
   */
  private static final boolean IS_NODEJS_EMBEDDER_BUNDLE_AVAILABLE = BundleUtils.isBundleInstalledWithMinVersion(
    "org.eclipse.wildwebdeveloper.embedder.node", 1, 0, 3);

  private NodeJsService() {
    // Utility class
  }

  private static boolean isNodeJsManagerPresent() {
    try {
      Class.forName("org.eclipse.wildwebdeveloper.embedder.node.NodeJSManager");
      return true;
    } catch (ClassNotFoundException ignored) {
      return false;
    }
  }

  @Nullable
  public static Path getNodeJsPath() {
    var path = SonarLintGlobalConfiguration.getNodejsPath();
    if (path != null) {
      return path;
    }

    if (IS_NODEJS_EMBEDDER_BUNDLE_AVAILABLE && isNodeJsManagerPresent()) {
      return EmbedderNodeJsUtils.getNodeJsPath();
    }

    return null;
  }
}
