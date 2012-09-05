/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2012 SonarSource
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
package org.sonar.ide.eclipse.internal.core;

import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.core.ISonarConnectionTester;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.WSClientFactory;
import org.sonar.wsclient.connectors.ConnectionException;
import org.sonar.wsclient.services.MetricQuery;
import org.sonar.wsclient.services.Server;
import org.sonar.wsclient.services.ServerQuery;
import org.sonar.wsclient.services.UserPropertyQuery;

public class ServerConnectionTester implements ISonarConnectionTester {
  public TestResult testSonar(String url, String user, String password) {
    Sonar sonar = getSonar(url, user, password);

    try {
      checkUrl(sonar);
    } catch (ConnectionException e) {
      LoggerFactory.getLogger(getClass()).error("Unable to connect", e);
      return TestResult.CONNECT_ERROR;
    }

    try {
      checkAuthentication(sonar, user, password);
    } catch (ConnectionException e) {
      return TestResult.AUTHENTICATION_ERROR;
    }

    return TestResult.OK;
  }

  private Sonar getSonar(String url, String user, String password) {
    return WSClientFactory.create(new Host(url, user, password));
  }

  private void checkAuthentication(Sonar sonar, String user, String password) {
    sonar.find(MetricQuery.all()); // Will succeed only if user/password are ok OR user/password are wrong but anonymous access is on

    if (!StringUtils.isBlank(user) || !StringUtils.isBlank(password)) {
      sonar.find(new UserPropertyQuery()); // Will succeed only if user/password are ok
    }
  }

  private void checkUrl(Sonar sonar) {
    Server server = sonar.find(new ServerQuery());

    LoggerFactory.getLogger(getClass()).info("Connected to Sonar " + server.getVersion());
  }
}
