/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.ide.eclipse.core.internal.remote;

import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.servers.SonarServer;
import org.sonar.ide.eclipse.wsclient.ConnectionException;
import org.sonar.ide.eclipse.wsclient.ISonarWSClientFacade;
import org.sonar.ide.eclipse.wsclient.WSClientFactory;

/**
 * EXPERIMENTAL!!!
 * Layer between Sonar IDE and Sonar based on sonar-ws-client :
 * Sonar IDE -> RemoteSonarIndex -> sonar-ws-client -> Sonar
 *
 * @author Evgeny Mandrikov
 * @since 0.2
 */
class RemoteSonarIndex {

  private final ISonarWSClientFacade sonarClient;
  private final SimpleSourceCodeDiffEngine diffEngine;
  private final SonarServer sonarServer;

  public RemoteSonarIndex(SonarServer sonarServer, SimpleSourceCodeDiffEngine diffEngine) {
    this.sonarClient = WSClientFactory.getSonarClient(sonarServer);
    this.diffEngine = diffEngine;
    this.sonarServer = sonarServer;
  }

  /**
   * {@inheritDoc}
   */
  public SourceCode search(String key) {
    try {
      if (sonarClient.exists(key)) {
        return new RemoteSourceCode(key).setRemoteSonarIndex(this);
      }
    } catch (ConnectionException e) {
      SonarCorePlugin.getDefault().info("Unable to connect to " + sonarServer.getUrl() + ". Server will be disabled.");
      sonarServer.setDisabled(true);
    }
    return null;
  }

  protected ISonarWSClientFacade getSonarClient() {
    return sonarClient;
  }

  protected SimpleSourceCodeDiffEngine getDiffEngine() {
    return diffEngine;
  }

}
