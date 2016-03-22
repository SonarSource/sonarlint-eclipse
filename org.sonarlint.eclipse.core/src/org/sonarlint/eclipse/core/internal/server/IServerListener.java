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
 * This interface is used by the server to broadcast a change of state.
 * Usually, the change of state will be caused by some user action,
 * (e.g. requesting to start a server) however, it is equally fine for
 * a server to broadcast a change of state through no direct user action.
 * (e.g. stopping because the server crashed) This information can be
 * used to inform the user of the change or update the UI.
 *
 * <p>Note: The server listener event MUST NOT directly be used to modify
 * the server's or module's state via one of the server's method. For example, 
 * a server stopped event cannot directly trigger a start(). Doing this may 
 * cause the thread to hang.</p>
 *   
 * @since 1.0
 */
public interface IServerListener {
  /**
   * A server has been changed as specified in the event.
   * 
   * @param event a server event that contains information on the change
   */
  void serverChanged(IServer server);
}
