/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.core;

import java.util.Collection;
import java.util.List;

import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;

/**
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ISonarServersManager {

  Collection<SonarServer> getServers();

  // TODO From old implementation, so should be reviewed :

  public List<Host> getHosts();

  void removeServer(String host);

  void addServer(String location, String username, String password);

  Host findServer(String host);

  Sonar getSonar(String url);

  boolean testSonar(String url, String user, String password) throws Exception;

}
