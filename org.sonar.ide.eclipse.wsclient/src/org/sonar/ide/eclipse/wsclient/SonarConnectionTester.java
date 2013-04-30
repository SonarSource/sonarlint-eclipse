/*
 * Sonar Eclipse
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
package org.sonar.ide.eclipse.wsclient;

import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.connectors.ConnectionException;
import org.sonar.wsclient.services.Authentication;
import org.sonar.wsclient.services.AuthenticationQuery;

public class SonarConnectionTester {
  public static enum ConnectionTestResult {
    OK, CONNECT_ERROR, AUTHENTICATION_ERROR;
  }

  public ConnectionTestResult testSonar(String url, String user, String password) {
    try {
      Sonar sonar = getSonar(url,
          StringUtils.isNotEmpty(user) ? user : null,
          StringUtils.isNotEmpty(password) ? password : null);
      Authentication auth = sonar.find(new AuthenticationQuery());
      if (auth.isValid()) {
        return ConnectionTestResult.OK;
      }
      else {
        return ConnectionTestResult.AUTHENTICATION_ERROR;
      }
    } catch (ConnectionException e) {
      LoggerFactory.getLogger(getClass()).error("Unable to connect", e);
      return ConnectionTestResult.CONNECT_ERROR;
    }
  }

  private Sonar getSonar(String url, String user, String password) {
    return WSClientFactory.create(new Host(url, user, password));
  }

}
