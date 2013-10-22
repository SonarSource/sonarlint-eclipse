/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2013 SonarSource
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
package org.sonar.ide.eclipse.core.internal.remote;

import org.sonar.ide.eclipse.common.servers.ISonarServer;
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

  public RemoteSonarIndex(ISonarServer sonarServer, SimpleSourceCodeDiffEngine diffEngine) {
    this(WSClientFactory.getSonarClient(sonarServer), diffEngine);
  }

  private RemoteSonarIndex(ISonarWSClientFacade sonarClient, SimpleSourceCodeDiffEngine diffEngine) {
    this.sonarClient = sonarClient;
    this.diffEngine = diffEngine;
  }

  /**
   * {@inheritDoc}
   */
  public SourceCode search(String key) {
    if (sonarClient.exists(key)) {
      return new RemoteSourceCode(key).setRemoteSonarIndex(this);
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
