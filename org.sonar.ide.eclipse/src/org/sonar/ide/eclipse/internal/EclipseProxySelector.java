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

package org.sonar.ide.eclipse.internal;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;

/**
 * {@link ProxySelector}, which selects the proxy server to use via {@link IProxyService}.
 * 
 * @author Evgeny Mandrikov
 */
public class EclipseProxySelector extends ProxySelector {

  private final IProxyService service;

  public EclipseProxySelector(IProxyService service) {
    this.service = service;
  }

  @Override
  public List<Proxy> select(final URI uri) {
    final ArrayList<Proxy> result = new ArrayList<Proxy>();

    final String host = uri.getHost();

    String type = IProxyData.SOCKS_PROXY_TYPE;
    if ("http".equals(uri.getScheme())) {
      type = IProxyData.HTTP_PROXY_TYPE;
    } else if ("ftp".equals(uri.getScheme())) {
      type = IProxyData.HTTP_PROXY_TYPE;
    } else if ("https".equals(uri.getScheme())) {
      type = IProxyData.HTTPS_PROXY_TYPE;
    }

    // TODO Godin: by some reasons service.select(uri) doesn't work here
    final IProxyData data = service.getProxyDataForHost(host, type);
    if (data != null) {
      if (IProxyData.HTTP_PROXY_TYPE.equals(data.getType())) {
        addProxy(result, Proxy.Type.HTTP, data);
      } else if (IProxyData.HTTPS_PROXY_TYPE.equals(data.getType())) {
        addProxy(result, Proxy.Type.HTTP, data);
      } else if (IProxyData.SOCKS_PROXY_TYPE.equals(data.getType())) {
        addProxy(result, Proxy.Type.SOCKS, data);
      }
    }

    if (result.isEmpty()) {
      result.add(Proxy.NO_PROXY);
    }
    return result;
  }

  private void addProxy(final ArrayList<Proxy> list, final Proxy.Type type, final IProxyData d) {
    try {
      list.add(new Proxy(type, new InetSocketAddress(InetAddress.getByName(d.getHost()), d.getPort())));
    } catch (UnknownHostException uhe) {
      // Oh well.
    }
  }

  @Override
  public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
    // Don't tell Eclipse.
  }

}
