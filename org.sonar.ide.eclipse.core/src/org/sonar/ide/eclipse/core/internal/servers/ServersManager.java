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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.security.storage.EncodingUtils;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.sonar.ide.eclipse.common.servers.ISonarServer;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.wsclient.WSClientFactory;

public class ServersManager implements ISonarServersManager {
  static final String PREF_SERVERS = "servers";

  private Map<String, String> serverVersionCache = new HashMap<String, String>();

  @VisibleForTesting
  public Map<String, String> getServerVersionCache() {
    return serverVersionCache;
  }

  @Override
  public Collection<ISonarServer> getServers() {
    IEclipsePreferences rootNode = new InstanceScope().getNode(SonarCorePlugin.PLUGIN_ID);
    List<ISonarServer> servers = Lists.newArrayList();
    try {
      rootNode.sync();
      if (rootNode.nodeExists(PREF_SERVERS)) {
        Preferences serversNode = rootNode.node(PREF_SERVERS);
        for (String encodedUrl : serversNode.childrenNames()) {
          Preferences serverNode = serversNode.node(encodedUrl);
          String url = EncodingUtils.decodeSlashes(encodedUrl);
          boolean auth = serverNode.getBoolean("auth", false);
          SonarServer sonarServer = new SonarServer(url, auth);
          if (!serverVersionCache.containsKey(sonarServer.getUrl())) {
            String serverVersion = getServerVersion(sonarServer);
            if (serverVersion != null) {
              serverVersionCache.put(sonarServer.getUrl(), serverVersion);
            }
          }
          sonarServer.setVersion(serverVersionCache.get(sonarServer.getUrl()));
          servers.add(sonarServer);
        }
      } else {
        // Defaults
        return Arrays.asList((ISonarServer) new SonarServer("http://localhost:9000"));
      }
    } catch (BackingStoreException e) {
      SonarCorePlugin.getDefault().error(e.getMessage(), e);
    }
    return servers;
  }

  @Override
  public void addServer(ISonarServer server) {
    String encodedUrl = EncodingUtils.encodeSlashes(server.getUrl());
    IEclipsePreferences rootNode = new InstanceScope().getNode(SonarCorePlugin.PLUGIN_ID);
    try {
      Preferences serversNode = rootNode.node(PREF_SERVERS);
      serversNode.put("initialized", "true");
      serversNode.node(encodedUrl).putBoolean("auth", server.hasCredentials());
      serversNode.flush();
    } catch (BackingStoreException e) {
      SonarCorePlugin.getDefault().error(e.getMessage(), e);
    }
  }

  /**
   * For tests.
   */
  public void clean() {
    IEclipsePreferences rootNode = new InstanceScope().getNode(SonarCorePlugin.PLUGIN_ID);
    try {
      rootNode.node(PREF_SERVERS).removeNode();
      rootNode.node(PREF_SERVERS).put("initialized", "true");
      rootNode.flush();
    } catch (BackingStoreException e) {
      SonarCorePlugin.getDefault().error(e.getMessage(), e);
    }
    serverVersionCache.clear();
  }

  @Override
  public void removeServer(ISonarServer server) {
    String encodedUrl = EncodingUtils.encodeSlashes(server.getUrl());
    IEclipsePreferences rootNode = new InstanceScope().getNode(SonarCorePlugin.PLUGIN_ID);
    try {
      Preferences serversNode = rootNode.node(PREF_SERVERS);
      serversNode.node(encodedUrl).removeNode();
      serversNode.flush();
    } catch (BackingStoreException e) {
      SonarCorePlugin.getDefault().error(e.getMessage(), e);
    }
    serverVersionCache.remove(server.getUrl());
  }

  @CheckForNull
  @Override
  public ISonarServer findServer(String url) {
    for (ISonarServer server : getServers()) {
      if (server.getUrl().equals(url)) {
        return server;
      }
    }
    return null;
  }

  @Override
  public ISonarServer getDefault() {
    return new SonarServer("http://localhost:9000");
  }

  @Override
  public ISonarServer create(String location, String username, String password) {
    return new SonarServer(location, username, password);
  }

  @CheckForNull
  private String getServerVersion(ISonarServer server) {
    try {
      return WSClientFactory.getSonarClient(server).getServerVersion();
    } catch (Exception e) {
      SonarCorePlugin.getDefault().error("Unable to get version of server " + server.getUrl() + ": " + e.getMessage() + "\n");
    }
    return null;
  }

}
