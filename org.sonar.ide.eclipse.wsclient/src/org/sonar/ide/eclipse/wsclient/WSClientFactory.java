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

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.eclipse.core.net.proxy.IProxyData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.wsclient.internal.WSClientPlugin;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.connectors.HttpClient4Connector;

import java.net.URI;

public final class WSClientFactory {

  private static final Logger LOG = LoggerFactory.getLogger(WSClientFactory.class);

  private WSClientFactory() {
  }

  /**
   * Creates Sonar web service client, which uses proxy settings from Eclipse.
   */
  public static Sonar create(Host host) {
    HttpClient4Connector connector = new HttpClient4Connector(host);
    configureProxy(connector.getHttpClient(), host);
    return new Sonar(connector);
  }

  /**
   * Workaround for http://jira.codehaus.org/browse/SONAR-1586
   */
  private static void configureProxy(DefaultHttpClient httpClient, Host server) {
    try {
      IProxyData[] proxyDatas = WSClientPlugin.selectProxy(new URI(server.getHost()));
      for (IProxyData proxyData : proxyDatas) {
        LOG.debug("Proxy for [{}] - [{}]", server.getHost(), proxyData);
        HttpHost proxy = new HttpHost(proxyData.getHost(), proxyData.getPort());
        httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        if (proxyData.isRequiresAuthentication()) {
          httpClient.getCredentialsProvider().setCredentials(
              new AuthScope(proxyData.getHost(), proxyData.getPort()),
              new UsernamePasswordCredentials(proxyData.getUserId(), proxyData.getPassword()));
        }
        return;
      }
      LOG.debug("No proxy for [{}]", server.getHost());
    } catch (Exception e) {
      LOG.error("Unable to configure proxy for sonar-ws-client", e);
    }
  }

}
