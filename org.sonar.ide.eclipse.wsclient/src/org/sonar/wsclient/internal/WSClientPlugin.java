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
package org.sonar.wsclient.internal;

import java.net.Authenticator;
import java.net.ProxySelector;



import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class WSClientPlugin extends Plugin {

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);

    setupProxy(context);
  }

  private void setupProxy(final BundleContext context) {
    ServiceReference proxyServiceReference = context.getServiceReference(IProxyService.class.getName());
    if (proxyServiceReference != null) {
      IProxyService proxyService = (IProxyService) context.getService(proxyServiceReference);
      ProxySelector.setDefault(new EclipseProxySelector(proxyService));
      Authenticator.setDefault(new EclipseProxyAuthenticator(proxyService));
    }
  }

}
