/*
 * Sonar Eclipse
 * Copyright (C) 2010-2012 SonarSource
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
package org.sonar.wsclient;

import org.slf4j.LoggerFactory;
import org.sonar.wsclient.connectors.ConnectionException;
import org.sonar.wsclient.services.MetricQuery;
import org.sonar.wsclient.services.Server;
import org.sonar.wsclient.services.ServerQuery;
import org.sonar.wsclient.services.UserPropertyQuery;

public class SonarConnectionTester {
  public static enum ConnectionTestResult {
    OK, CONNECT_ERROR, AUTHENTICATION_ERROR;
  }

  public ConnectionTestResult testSonar(String url, String user, String password) {
    Sonar sonar = getSonar(url, user, password);

    try {
      checkUrl(sonar);
    } catch (ConnectionException e) {
      LoggerFactory.getLogger(getClass()).error("Unable to connect", e);
      return ConnectionTestResult.CONNECT_ERROR;
    }

    try {
      checkAuthentication(sonar, user, password);
    } catch (ConnectionException e) {
      return ConnectionTestResult.AUTHENTICATION_ERROR;
    }

    return ConnectionTestResult.OK;
  }

  private Sonar getSonar(String url, String user, String password) {
    return WSClientFactory.create(new Host(url, user, password));
  }

  private void checkAuthentication(Sonar sonar, String user, String password) {
    sonar.find(MetricQuery.all()); // Will succeed only if user/password are ok OR user/password are wrong but anonymous access is on

    if (!"".equals(user) || !"".equals(password)) {
      sonar.find(new UserPropertyQuery()); // Will succeed only if user/password are ok
    }
  }

  private void checkUrl(Sonar sonar) {
    Server server = sonar.find(new ServerQuery());

    LoggerFactory.getLogger(getClass()).info("Connected to Sonar " + server.getVersion());
  }
}
