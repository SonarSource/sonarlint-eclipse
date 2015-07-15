/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
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
package org.sonar.ide.eclipse.core.internal.servers;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.CheckForNull;
import org.sonar.ide.eclipse.common.servers.ISonarServer;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.wsclient.ISonarWSClientFacade;
import org.sonar.ide.eclipse.wsclient.WSClientFactory;

/**
 * {@link ISonarServer} version details encapsulated separately here from {@link ServersManager}.<br>
 * Lazy loading cacher for holding {@link ISonarServer} version.
 *
 * @see #get(ISonarServer)
 *
 * @author Hemantkumar Chigadani
 */
final class Version {

  private static final Map<String, Version> serverVersionCache = new HashMap<String, Version>();

  private final AtomicReference<String> v;

  private Version() {

    v = new AtomicReference<String>();
  }

  /**
   * @param sonarServer
   * @return the version
   */
  String get(final ISonarServer sonarServer) {
    String serverVersion = v.get();
    if (serverVersion == null) {
      serverVersion = getServerVersion(sonarServer);
      v.set(serverVersion);
    }
    return serverVersion;
  }

  /**
   * Silence version fetcher.
   *
   * @param server
   * @return
   */
  @SuppressWarnings("nls")
  @CheckForNull
  private String getServerVersion(final ISonarServer server) {
    String serverVersion = null;
    try {
      final ISonarWSClientFacade sonarClient = WSClientFactory.getSonarClient(server);
      serverVersion = sonarClient.getServerVersion();
    } catch (final Exception e) {
      SonarCorePlugin.getDefault().debug("Unable to get version of server " + server.getUrl() + ": " + e.getMessage() + "\n");
    }
    return serverVersion;
  }

  /**
   * @param url {@link ISonarServer} url location.
   * @return Cached version , if not found creates new version object.
   */
  static synchronized Version getVersion(final String url) {
    Version version = serverVersionCache.get(url);
    if (version == null) {
      version = new Version();
      serverVersionCache.put(url, version);
    }
    return version;
  }

  /**
   * @param server
   */
  static synchronized void remove(final ISonarServer server) {
    serverVersionCache.remove(server.getUrl());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "Version [v=" + v + "]";
  }

}
