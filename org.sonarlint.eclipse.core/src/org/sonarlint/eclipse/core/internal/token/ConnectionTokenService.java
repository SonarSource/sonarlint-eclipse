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
package org.sonarlint.eclipse.core.internal.token;

import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.internal.extension.SonarLintExtensionTracker;
import org.sonarlint.eclipse.core.internal.utils.BundleUtils;

public class ConnectionTokenService {
  /**
   *  We need to check that the bundle is present on the OSGi runtime and that the classes are available on the
   *  classpath. If only the first is present, this means the classes were compiled with a Java version higher than the
   *  one we currently rely on.
   */
  private static final boolean IS_ECLIPSE_EQUINOX_SECURITY_PRESENT = BundleUtils.isBundleInstalled(
    "org.eclipse.equinox.security");
  private static final boolean ARE_ECLIPSE_EQUINOX_SECURITY_CLASSES_PRESENT = isEclipseEquinoxPresent();

  private ConnectionTokenService() {
    // utility class
  }

  private static boolean isEclipseEquinoxPresent() {
    try {
      Class.forName("org.eclipse.equinox.security.storage.ISecurePreferences");
      Class.forName("org.eclipse.equinox.security.storage.SecurePreferencesFactory");
      Class.forName("org.eclipse.equinox.security.storage.StorageException");
      return true;
    } catch (ClassNotFoundException ignored) {
      return false;
    }
  }

  @Nullable
  public static String getToken(String connectionId) {
    // We try to get the token from a plug-in implementing the extension point first. If one is present, this means
    // indirectly that Eclipse Equinox Security is not available.
    for (var connectionTokenProvider : SonarLintExtensionTracker.getInstance().getConnectionTokenProviders()) {
      var tokenNullable = connectionTokenProvider.getToken(connectionId);
      if (tokenNullable != null) {
        return tokenNullable;
      }
    }

    if (!IS_ECLIPSE_EQUINOX_SECURITY_PRESENT || !ARE_ECLIPSE_EQUINOX_SECURITY_CLASSES_PRESENT) {
      throw new IllegalStateException("Eclipse Equinox Security is not present to get the token and no other token "
        + "providing plug-in was found in the IDE!");
    }
    return EclipseEquinoxSecurityUtils.getToken(connectionId);
  }

  public static boolean setToken(String connectionId, String token) {
    // We try to get set token using a plug-in implementing the extension point first. If one is present, this means
    // indirectly that Eclipse Equinox Security is not available.
    for (var connectionTokenProvider : SonarLintExtensionTracker.getInstance().getConnectionTokenProviders()) {
      var ret = connectionTokenProvider.setToken(connectionId, token);
      if (ret) {
        return true;
      }
    }

    if (!IS_ECLIPSE_EQUINOX_SECURITY_PRESENT || !ARE_ECLIPSE_EQUINOX_SECURITY_CLASSES_PRESENT) {
      throw new IllegalStateException("Eclipse Equinox Security is not present to set the token and no other token "
        + "providing plug-in was found in the IDE!");
    }
    return EclipseEquinoxSecurityUtils.setToken(connectionId, token);
  }

  public static void removeToken(String connectionId) {
    for (var connectionTokenProvider : SonarLintExtensionTracker.getInstance().getConnectionTokenProviders()) {
      connectionTokenProvider.removeToken(connectionId);
    }

    if (IS_ECLIPSE_EQUINOX_SECURITY_PRESENT && ARE_ECLIPSE_EQUINOX_SECURITY_CLASSES_PRESENT) {
      EclipseEquinoxSecurityUtils.removeToken(connectionId);
    }
  }
}
