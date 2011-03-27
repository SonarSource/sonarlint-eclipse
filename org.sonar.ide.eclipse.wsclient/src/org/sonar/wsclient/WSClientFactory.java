/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2011 SonarSource
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
package org.sonar.wsclient;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.wsclient.connectors.HttpClient3Connector;

import java.net.*;
import java.util.List;

public final class WSClientFactory {

  private static final Logger LOG = LoggerFactory.getLogger(WSClientFactory.class);

  private static final int TIMEOUT_MS = 30000;
  private static final int MAX_TOTAL_CONNECTIONS = 40;
  private static final int MAX_HOST_CONNECTIONS = 4;

  private WSClientFactory() {
  }

  /**
   * Creates Sonar web service client, which uses proxy settings from Eclipse.
   */
  public static Sonar create(Host host) {
    HttpClient httpClient = createHttpClient(host);
    configureCredentials(httpClient, host);
    configureProxy(httpClient, host);
    return new Sonar(new HttpClient3Connector(host, createHttpClient(host)));
  }

  /**
   * @see org.sonar.wsclient.connectors.HttpClient3Connector#createClient()
   */
  private static HttpClient createHttpClient(Host server) {
    final HttpConnectionManagerParams params = new HttpConnectionManagerParams();
    params.setConnectionTimeout(TIMEOUT_MS);
    params.setSoTimeout(TIMEOUT_MS);
    params.setDefaultMaxConnectionsPerHost(MAX_HOST_CONNECTIONS);
    params.setMaxTotalConnections(MAX_TOTAL_CONNECTIONS);
    final MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
    connectionManager.setParams(params);
    return new HttpClient(connectionManager);
  }

  /**
   * Workaround for http://jira.codehaus.org/browse/SONAR-1586
   */
  private static void configureProxy(HttpClient httpClient, Host server) {
    // TODO Godin: ugly hack to use ProxySelector with commons-httpclient 3.1
    try {
      List<Proxy> list = ProxySelector.getDefault().select(new URI(server.getHost()));
      for (Proxy proxy : list) {
        if (proxy.type() == Proxy.Type.HTTP) {
          // Proxy
          InetSocketAddress addr = (InetSocketAddress) proxy.address();
          httpClient.getHostConfiguration().setProxy(addr.getHostName(), addr.getPort());
          // Proxy auth
          InetAddress proxyInetAddress = InetAddress.getByName(addr.getHostName());
          PasswordAuthentication auth = Authenticator.requestPasswordAuthentication(proxyInetAddress, addr.getPort(), null, null, null);
          if (auth != null) {
            Credentials defaultcreds = new UsernamePasswordCredentials(auth.getUserName(), new String(auth.getPassword()));
            httpClient.getState().setProxyCredentials(AuthScope.ANY, defaultcreds);
          }
        }
      }
    } catch (Exception e) {
      LOG.error("Unable to configure proxy for sonar-ws-client", e);
    }
  }

  /**
   * TODO Godin: I suppose that call of method {@link HttpClient3Connector#configureCredentials()} can be added to constructor {@link HttpClient3Connector#HttpClient3Connector(Host, HttpClient)}
   */
  private static void configureCredentials(HttpClient httpClient, Host server) {
    String username = server.getUsername();
    if (username != null && !"".equals(username)) {
      httpClient.getParams().setAuthenticationPreemptive(true);
      Credentials defaultcreds = new UsernamePasswordCredentials(server.getUsername(), server.getPassword());
      httpClient.getState().setCredentials(AuthScope.ANY, defaultcreds);
    }
  }

}
