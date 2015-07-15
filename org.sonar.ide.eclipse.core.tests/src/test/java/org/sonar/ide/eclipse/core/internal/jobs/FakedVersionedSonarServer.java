/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.core.internal.jobs;

import org.sonar.ide.eclipse.common.servers.ISonarServer;

/**
 * Dummy implementation of ISonarServer.
 * 
 * @author Hemantkumar Chigadani
 */
class FakedVersionedSonarServer implements ISonarServer {

  private final ISonarServer server;
  private final String version;

  /**
   * @param server
   * @param version
   *
   */
  public FakedVersionedSonarServer(final ISonarServer server, final String version) {
    this.server = server;
    this.version = version;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getUrl() {
    return server.getUrl();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasCredentials() {
    return server.hasCredentials();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getUsername() {
    return server.getUsername();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getPassword() {
    return server.getPassword();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getVersion() {
    return this.version;
  }

}
