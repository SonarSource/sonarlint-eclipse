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
package org.sonar.wsclient.internal;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import java.net.URI;

public class WSClientPlugin extends Plugin {

  private static WSClientPlugin plugin;

  public WSClientPlugin() {
    plugin = this; // NOSONAR
  }

  public static IProxyData[] selectProxy(URI uri) {
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
