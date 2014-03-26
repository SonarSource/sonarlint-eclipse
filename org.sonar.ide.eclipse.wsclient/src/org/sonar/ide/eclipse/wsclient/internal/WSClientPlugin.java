/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2014 SonarSource
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
package org.sonar.ide.eclipse.wsclient.internal;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class WSClientPlugin extends Plugin {
  private static final Logger LOG = LoggerFactory.getLogger(WSClientPlugin.class);

  private static WSClientPlugin plugin;

  public WSClientPlugin() {
    plugin = this;
  }

  public static IProxyData[] selectProxy(URI uri) {
    LOG.debug("Searching proxy for uri [" + uri + "]");

    if ((null == uri) || ("".equals(uri.toString()))) {
      return new IProxyData[0];
    }

    BundleContext context = plugin.getBundle().getBundleContext();

    ServiceReference proxyServiceReference = context.getServiceReference(IProxyService.class.getName());
    if (proxyServiceReference == null) {
      return new IProxyData[0];
    }

    @SuppressWarnings("unchecked")
    IProxyService service = (IProxyService) context.getService(proxyServiceReference);
    return service.select(uri);
  }
}
