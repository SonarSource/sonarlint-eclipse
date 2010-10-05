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

package org.sonar.ide.eclipse;

import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.UnknownHostException;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;

/**
 * {@link Authenticator}, which works via {@link IProxyService}.
 * 
 * @author Evgeny Mandrikov
 */
public class EclipseProxyAuthenticator extends Authenticator {

  private final IProxyService service;

  public EclipseProxyAuthenticator(IProxyService service) {
    this.service = service;
  }

  @Override
  protected PasswordAuthentication getPasswordAuthentication() {
    final IProxyData[] data = service.getProxyData();
    if (data == null) {
      return null;
    }
    for (final IProxyData d : data) {
      if (d.getUserId() == null || d.getHost() == null) {
        continue;
      }
      if (d.getPort() == getRequestingPort() && hostMatches(d)) {
        return auth(d);
      }
    }
    return null;
  }

  private PasswordAuthentication auth(final IProxyData d) {
    final String user = d.getUserId();
    final String pass = d.getPassword();
    final char[] passChar = pass != null ? pass.toCharArray() : new char[0];
    return new PasswordAuthentication(user, passChar);
  }

  private boolean hostMatches(final IProxyData d) {
    try {
      final InetAddress dHost = InetAddress.getByName(d.getHost());
      InetAddress rHost = getRequestingSite();
      if (rHost == null) {
        rHost = InetAddress.getByName(getRequestingHost());
      }
      return dHost.equals(rHost);
    } catch (UnknownHostException err) {
      return false;
    }
  }
}
