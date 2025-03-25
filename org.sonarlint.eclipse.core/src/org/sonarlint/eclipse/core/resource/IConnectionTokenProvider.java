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
package org.sonarlint.eclipse.core.resource;

import org.eclipse.jdt.annotation.Nullable;

/**
 *  By default we try to store tokens for Connected Mode connections in the Eclipse Equinox Secure storage. Some
 *  Eclipse-based IDEs do not come bundled with the Eclipse plug-in providing this logic, or users might want to store
 *  the credentials differently. In these cases this extension point should be implemented in a third-party plug-in to
 *  enable the Connected Mode.
 *
 *  @since 11.4
 */
public interface IConnectionTokenProvider {
  /**
   *  This is used to get the token for a specific Connected Mode connection denoted by its id. When the connection
   *  does not exist, it is not available.
   *
   *  @param connectionId as the unique identifier for the token to be stored
   *  @return token if available, null otherwise
   */
  @Nullable
  String getToken(String connectionId);

  /**
   *  This is used to set the token for a specific Connected Mode connection denoted by its id. When the connection
   *  does not exist, it is created, otherwise overwritten.
   *
   *  @param connectionId as the unique identifier for the token to be stored
   *  @param token to be stored
   *  @return true when the token was stored, false otherwise
   */
  boolean setToken(String connectionId, String token);

  /**
   *  This is used to remove the token for a specific Connected Mode connection denoted by its id. When the connection
   *  does not exist, nothing happens.
   *
   *  @param connectionId as the unique identifier for the token to be removed
   */
  void removeToken(String connectionId);
}
