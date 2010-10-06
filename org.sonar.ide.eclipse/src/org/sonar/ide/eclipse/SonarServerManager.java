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

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.security.storage.EncodingUtils;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.sonar.ide.client.SonarClient;
import org.sonar.ide.eclipse.core.ISonarConstants;
import org.sonar.ide.eclipse.core.SonarLogger;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;

import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

public class SonarServerManager {

  protected SonarServerManager() {
  }

  public void addServer(String location, String username, String password) throws Exception {
    addServer(new Host(location, username, password));
  }

  public void addServer(Host server) {
    Preferences preferences = getPreferences();
    String encodedUrl = EncodingUtils.encodeSlashes(server.getHost());
    preferences = preferences.node(encodedUrl);
    preferences.put("username", server.getUsername());
    preferences.put("password", server.getPassword());
  }

  // private ISecurePreferences getSecurePreferences() {
  // return SecurePreferencesFactory.getDefault().node(ISonarConstants.PLUGIN_ID);
  // }

  private Preferences getPreferences() {
    return new InstanceScope().getNode(ISonarConstants.PLUGIN_ID).node("servers");
  }

  public List<Host> getServers() {
    List<Host> servers = Lists.newArrayList();
    try {
      Preferences preferences = getPreferences();
      for (String encodedUrl : preferences.childrenNames()) {
        Preferences serverNode = preferences.node(encodedUrl);
        String url = EncodingUtils.decodeSlashes(encodedUrl);
        String username = serverNode.get("username", "");
        String password = serverNode.get("password", "");
        servers.add(new Host(url, username, password));
      }
      return servers;
    } catch (BackingStoreException e) {
      SonarLogger.log(e);
      return Collections.emptyList();
    }
  }

  public boolean removeServer(String host) {
    Preferences preferences = getPreferences();
    String encodedUrl = EncodingUtils.encodeSlashes(host);
    try {
      if (preferences.nodeExists(encodedUrl)) {
        preferences.node(encodedUrl).removeNode();
      }
      return true;
    } catch (BackingStoreException e) {
      return false;
    }
  }

  /**
   * @deprecated since 0.3 use {@link #findServer(String)} instead
   */
  @Deprecated
  public Host createServer(String url) {
    return findServer(url);
  }

  public Host findServer(String host) {
    Preferences preferences = getPreferences();
    String encodedUrl = EncodingUtils.encodeSlashes(host);
    Host result;
    preferences = preferences.node(encodedUrl);
    String username = preferences.get("username", "");
    String password = preferences.get("password", "");
    result = new Host(host, username, password);
    return result;
  }

  public Sonar getSonar(String url) {
    final Host server = createServer(url);
    return new SonarClient(server.getHost(), server.getUsername(), server.getPassword());
  }

  public boolean testSonar(String url, String user, String password) throws Exception {
    SonarClient sonar = new SonarClient(url, user, password);
    return sonar.isAvailable();
  }

}
