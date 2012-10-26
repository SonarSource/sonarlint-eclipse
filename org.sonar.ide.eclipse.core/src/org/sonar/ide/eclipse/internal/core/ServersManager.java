/*
 * Sonar Eclipse
 * Copyright (C) 2010-2012 SonarSource
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
package org.sonar.ide.eclipse.internal.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.security.storage.EncodingUtils;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.core.ISonarServersManager;
import org.sonar.ide.eclipse.core.SonarServer;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.WSClientFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ServersManager implements ISonarServersManager {
  public static final String NODE = "org.sonar.ide.eclipse.core"; // plugin key
  private static final String PREF_SERVERS = "servers";

  private final Map<String, SonarServer> servers = Maps.newHashMap();

  public void load() {
    for (SonarServer server : loadServers()) {
      servers.put(server.getUrl(), server);
    }
  }

  public void save() {
    saveServers(servers.values());
  }

  private static Collection<SonarServer> loadServers() {
    IEclipsePreferences rootNode = new InstanceScope().getNode(NODE);
    List<SonarServer> servers = Lists.newArrayList();
    try {
      rootNode.sync();
      if (rootNode.nodeExists(PREF_SERVERS)) {
        Preferences serversNode = rootNode.node(PREF_SERVERS);
        for (String encodedUrl : serversNode.childrenNames()) {
          Preferences serverNode = serversNode.node(encodedUrl);
          String url = EncodingUtils.decodeSlashes(encodedUrl);
          boolean auth = serverNode.getBoolean("auth", false);
          servers.add(new SonarServer(url, auth));
        }
      } else {
        // Defaults
        return Arrays.asList(new SonarServer("http://localhost:9000"), new SonarServer("http://nemo.sonarsource.org"));
      }
    } catch (BackingStoreException e) {
      LoggerFactory.getLogger(SecurityManager.class).error(e.getMessage(), e);
    }
    return servers;
  }

  private static void saveServers(Collection<SonarServer> servers) {
    IEclipsePreferences rootNode = new InstanceScope().getNode(NODE);
    try {
      Preferences serversNode = rootNode.node(PREF_SERVERS);
      serversNode.put("initialized", "true");
      for (String child : serversNode.childrenNames()) {
        serversNode.node(child).removeNode();
      }
      for (SonarServer server : servers) {
        String encodedUrl = EncodingUtils.encodeSlashes(server.getUrl());
        serversNode.node(encodedUrl).putBoolean("auth", server.hasCredentials());
      }
      serversNode.flush();
    } catch (BackingStoreException e) {
      LoggerFactory.getLogger(SecurityManager.class).error(e.getMessage(), e);
    }
  }

  public Collection<SonarServer> getServers() {
    return servers.values();
  }

  /**
   * For tests.
   */
  public void clean() {
    servers.clear();
  }

  // From old implementation

  public List<Host> getHosts() {
    List<Host> result = Lists.newArrayList();
    for (SonarServer server : getServers()) {
      result.add(server.getHost());
    }
    return result;
  }

  public void removeServer(String url) {
    servers.remove(url);
  }

  public void addServer(String url, String username, String password) {
    servers.put(url, new SonarServer(url, username, password));
  }

  public Host findServer(String url) {
    SonarServer server = servers.get(url);
    if (server == null) { // FIXME dirty hack
      addServer(url, "", "");
      return new Host(url, "", "");
    } else {
      return server.getHost();
    }
  }

  public Sonar getSonar(String url) {
    Host server = findServer(url);
    return WSClientFactory.create(server);
  }
}
