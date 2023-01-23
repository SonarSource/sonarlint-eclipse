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
package org.sonarlint.eclipse.core.internal.utils;

import java.util.Optional;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public class BundleUtils {

  public static boolean isBundleInstalled(String name) {
    return getInstalledBundle(name).isPresent();
  }

  public static boolean isBundleInstalledWithMinVersion(String name, int majorVersion, int minorVersion) {
    return getInstalledBundle(name)
      .map(bundle -> bundle.getVersion().compareTo(new Version(majorVersion, minorVersion, 0)) >= 0)
      .orElse(false);
  }

  public static Optional<Bundle> getInstalledBundle(String name) {
    try {
      var bundle = Platform.getBundle(name);
      if (bundle != null && (bundle.getState() & (Bundle.ACTIVE | Bundle.STARTING | Bundle.RESOLVED)) != 0) {
        return Optional.of(bundle);
      }
    } catch (Throwable exception) {
      // Assume that it's not available.
    }
    return Optional.empty();
  }

  private BundleUtils() {
    // utility class
  }
}
