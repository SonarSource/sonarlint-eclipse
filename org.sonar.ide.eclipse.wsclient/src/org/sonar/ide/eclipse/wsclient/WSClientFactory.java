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
package org.sonar.ide.eclipse.wsclient;

import java.net.URI;
import java.net.URISyntaxException;
import javax.annotation.Nullable;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.eclipse.core.net.proxy.IProxyData;
import org.sonar.ide.eclipse.common.servers.ISonarServer;
import org.sonar.ide.eclipse.wsclient.internal.SonarWSClientFacade;
import org.sonar.ide.eclipse.wsclient.internal.WSClientPlugin;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.SonarClient.Builder;
import org.sonar.wsclient.connectors.HttpClient4Connector;

public final class WSClientFactory {

  private WSClientFactory() {
  }

  /**
   * Creates Sonar web service client facade, which uses proxy settings from Eclipse.
   */
  @Nullable
  public static ISonarWSClientFacade getSonarClient(ISonarServer sonarServer) {
    if (sonarServer.disabled()) {
      return null;
    }
    Host host;
    if (sonarServer.hasCredentials()) {
      host = new Host(sonarServer.getUrl(), sonarServer.getUsername(), sonarServer.getPassword());
    } else {
      host = new Host(sonarServer.getUrl());
    }
    return new SonarWSClientFacade(create(host), createSonarClient(host));
  }

  /**
   * Creates Sonar web service client, which uses proxy settings from Eclipse.
   */
  private static Sonar create(Host host) {
    HttpClient4Connector connector = new HttpClient4Connector(host);
    configureProxy(connector.getHttpClient(), host);
    return new Sonar(connector);
  }

  /**
   * Creates new Sonar web service client, which uses proxy settings from Eclipse.
   */
  private static SonarClient createSonarClient(Host host) {
    Builder builder = SonarClient.builder()
      .url(host.getHost())
      .login(host.getUsername())
      .password(host.getPassword());
    IProxyData proxyData = getEclipseProxyFor(host);
    if (proxyData != null && !IProxyData.SOCKS_PROXY_TYPE.equals(proxyData.getType())) {
      builder.proxy(proxyData.getHost(), proxyData.getPort());
      if (proxyData.isRequiresAuthentication()) {
        builder.proxyLogin(proxyData.getUserId()).proxyPassword(proxyData.getPassword());
      }
    }
    return builder.build();
  }

  /**
   * Workaround for http://jira.codehaus.org/browse/SONAR-1586
   */
  private static void configureProxy(DefaultHttpClient httpClient, Host server) {
    IProxyData proxyData = getEclipseProxyFor(server);
    if (proxyData != null && !IProxyData.SOCKS_PROXY_TYPE.equals(proxyData.getType())) {
      HttpHost proxy = new HttpHost(proxyData.getHost(), proxyData.getPort());
      httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
      if (proxyData.isRequiresAuthentication()) {
        httpClient.getCredentialsProvider().setCredentials(
          new AuthScope(proxyData.getHost(), proxyData.getPort()),
          new UsernamePasswordCredentials(proxyData.getUserId(), proxyData.getPassword()));
      }
    }
  }

  private static IProxyData getEclipseProxyFor(Host server) {
    IProxyData[] proxyDatas;
    try {
      proxyDatas = WSClientPlugin.selectProxy(new URI(server.getHost()));
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
    return proxyDatas != null && proxyDatas.length > 0 ? proxyDatas[0] : null;
  }

}
