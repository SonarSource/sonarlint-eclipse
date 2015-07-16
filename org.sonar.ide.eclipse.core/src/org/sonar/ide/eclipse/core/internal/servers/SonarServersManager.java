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
package org.sonar.ide.eclipse.core.internal.servers;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import javax.annotation.CheckForNull;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.security.storage.EncodingUtils;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.sonar.ide.eclipse.common.servers.ISonarServer;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.wsclient.WSClientFactory;

public class SonarServersManager implements ISonarServersManager {
  private static final String INITIALIZED_ATTRIBUTE = "initialized";

  private static final String AUTH_ATTRIBUTE = "auth";

  private static final String URL_ATTRIBUTE = "url";

  static final String PREF_SERVERS = "servers";

  private List<ISonarServer> servers = Lists.newArrayList();

  private boolean loadedOnce = false;

  @Override
  public synchronized Collection<ISonarServer> reloadServers() {
    reloadFromEclipsePreferencesAndCheckStatus();
    return servers;
  }

  @Override
  public synchronized Collection<ISonarServer> getServers() {
    if (!loadedOnce) {
      reloadFromEclipsePreferencesAndCheckStatus();
    }
    return servers;
  }

  private void reloadFromEclipsePreferencesAndCheckStatus() {
    servers.clear();
    IEclipsePreferences rootNode = InstanceScope.INSTANCE.getNode(SonarCorePlugin.PLUGIN_ID);
    try {
      rootNode.sync();
      Preferences serversNode = rootNode.nodeExists(PREF_SERVERS) ? rootNode.node(PREF_SERVERS) : DefaultScope.INSTANCE.getNode(SonarCorePlugin.PLUGIN_ID).node(PREF_SERVERS);
      for (String idOrEncodedUrl : serversNode.childrenNames()) {
        Preferences serverNode = serversNode.node(idOrEncodedUrl);
        String id;
        String url = serverNode.get(URL_ATTRIBUTE, null);
        if (url != null) {
          id = EncodingUtils.decodeSlashes(idOrEncodedUrl);
        } else {
          url = EncodingUtils.decodeSlashes(idOrEncodedUrl);
          id = url;
        }
        boolean auth = serverNode.getBoolean(AUTH_ATTRIBUTE, false);
        SonarServer sonarServer = new SonarServer(id, url, auth);
        String serverVersion = getServerVersion(sonarServer);
        sonarServer.setVersion(serverVersion != null ? serverVersion : "<unknown>");
        sonarServer.setDisabled(serverVersion == null);
        servers.add(sonarServer);
      }
    } catch (BackingStoreException e) {
      SonarCorePlugin.getDefault().error(e.getMessage(), e);
    }
    loadedOnce = true;
  }

  @Override
  public synchronized void addServer(ISonarServer server) {
    IEclipsePreferences rootNode = InstanceScope.INSTANCE.getNode(SonarCorePlugin.PLUGIN_ID);
    try {
      Preferences serversNode = rootNode.node(PREF_SERVERS);
      serversNode.put(INITIALIZED_ATTRIBUTE, "true");
      Preferences serverNode = serversNode.node(EncodingUtils.encodeSlashes(server.getId()));
      serverNode.put(URL_ATTRIBUTE, server.getUrl());
      serverNode.putBoolean(AUTH_ATTRIBUTE, server.hasCredentials());
      serversNode.flush();
    } catch (BackingStoreException e) {
      SonarCorePlugin.getDefault().error(e.getMessage(), e);
    }
    reloadFromEclipsePreferencesAndCheckStatus();
  }

  /**
   * For tests.
   */
  public synchronized void clean() {
    servers.clear();
    IEclipsePreferences rootNode = InstanceScope.INSTANCE.getNode(SonarCorePlugin.PLUGIN_ID);
    try {
      rootNode.node(PREF_SERVERS).removeNode();
      rootNode.node(PREF_SERVERS).put(INITIALIZED_ATTRIBUTE, "true");
      rootNode.flush();
    } catch (BackingStoreException e) {
      SonarCorePlugin.getDefault().error(e.getMessage(), e);
    }
  }

  @Override
  public synchronized void removeServer(ISonarServer server) {
    IEclipsePreferences rootNode = InstanceScope.INSTANCE.getNode(SonarCorePlugin.PLUGIN_ID);
    try {
      Preferences serversNode = rootNode.node(PREF_SERVERS);
      serversNode.put(INITIALIZED_ATTRIBUTE, "true");
      serversNode.node(EncodingUtils.encodeSlashes(server.getId())).removeNode();
      serversNode.flush();
    } catch (BackingStoreException e) {
      SonarCorePlugin.getDefault().error(e.getMessage(), e);
    }
    reloadFromEclipsePreferencesAndCheckStatus();
  }

  @CheckForNull
  @Override
  public synchronized ISonarServer findServer(String idOrUrl) {
    for (ISonarServer server : servers) {
      if (server.getId().equals(idOrUrl) || server.getUrl().equals(idOrUrl)) {
        return server;
      }
    }
    return null;
  }

  @Override
  public ISonarServer create(String id, String location, String username, String password) {
    return new SonarServer(id, location, username, password);
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
