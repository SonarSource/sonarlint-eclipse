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

import java.io.IOException;
import java.util.Objects;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectionManager;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;

public class EclipseEquinoxSecurityUtils {
  // Even though this now storing only the token, we cannot rename the attribute as updating from
  // previous versions would not work anymore - keep compatibility!
  public static final String TOKEN_ATTRIBUTE = "username";

  private EclipseEquinoxSecurityUtils() {
    // utility class
  }

  @Nullable
  public static String getToken(String connectionId) {
    try {
      var connectionNodeName = StringUtils.urlEncode(connectionId);
      var secureConnectionsNode = getSecureConnectionsNode();
      var secureConnectionNode = secureConnectionsNode.node(connectionNodeName);
      return secureConnectionNode.get(TOKEN_ATTRIBUTE, null);
    } catch (StorageException e) {
      throw new IllegalStateException("Unable to retrieve connection credentials in secure storage: " + e.getMessage(), e);
    }
  }

  public static boolean setToken(String connectionId, String token) {
    try {
      var connectionNodeName = StringUtils.urlEncode(connectionId);
      var secureConnectionsNode = getSecureConnectionsNode();
      var secureConnectionNode = secureConnectionsNode.node(connectionNodeName);
      var previousToken = secureConnectionNode.get(TOKEN_ATTRIBUTE, null);
      secureConnectionNode.put(TOKEN_ATTRIBUTE, token, true);
      secureConnectionsNode.flush();
      return !Objects.equals(previousToken, token);
    } catch (StorageException | IOException e) {
      throw new IllegalStateException("Unable to store connection credentials in secure storage: " + e.getMessage(), e);
    }
  }

  public static void removeToken(String connectionId) {
    var connectionNodeName = StringUtils.urlEncode(connectionId);
    var secureConnectionsNode = getSecureConnectionsNode();
    secureConnectionsNode.node(connectionNodeName).removeNode();
  }

  private static ISecurePreferences getSecureConnectionsNode() {
    return SecurePreferencesFactory.getDefault()
      .node(SonarLintCorePlugin.PLUGIN_ID)
      .node(ConnectionManager.PREF_CONNECTIONS);
  }
}
