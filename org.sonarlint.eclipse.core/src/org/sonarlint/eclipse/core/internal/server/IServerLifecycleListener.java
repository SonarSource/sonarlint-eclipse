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
package org.sonarlint.eclipse.core.internal.server;

/**
 * Listener interface for changes to servers.
 * <p>
 * This interface is fired whenever a server is added, modified, or removed.
 * All events are fired post-change, so that all server tools API called as a
 * result of the event will return the updated results. (for example, on
 * serverAdded the new server will be in the global list of servers
 * ({@link ServerCore#getServers()}), and on serverRemoved the server will
 * not be in the list.
 * </p>
 * 
 * @see ServerCore
 * @see IServer
 * @since 1.0
 */
public interface IServerLifecycleListener {
  /**
   * A new server has been created.
   *
   * @param server the new server
   */
  void serverAdded(IServer server);

  /**
   * An existing server has been updated or modified.
   *
   * @param server the modified server
   */
  void serverChanged(IServer server);

  /**
   * A existing server has been removed.
   *
   * @param server the removed server
   */
  void serverRemoved(IServer server);
}
