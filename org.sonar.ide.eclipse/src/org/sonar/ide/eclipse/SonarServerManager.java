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

import org.eclipse.equinox.security.storage.EncodingUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.sonar.ide.client.SonarClient;
import org.sonar.ide.eclipse.core.ISonarConstants;
import org.sonar.ide.eclipse.core.SonarLogger;
import org.sonar.ide.eclipse.core.SonarServer;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

public class SonarServerManager {

  private static boolean useSecureStorage = true;

  /**
   * For tests
   */
  public static void enableSecureStorate(boolean enable) {
    useSecureStorage = enable;
  }

  /**
   * For tests
   */
  public void clean() {
    servers.clear();
  }

  private Map<String, Host> servers;

  protected SonarServerManager() {
    servers = Maps.newHashMap();
    if (useSecureStorage) {
      loadServers();
    }
  }

  private void loadServers() {
    ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault().node(ISonarConstants.PLUGIN_ID);
    for (String encodedUrl : securePreferences.childrenNames()) {
      ISecurePreferences serverNode = securePreferences.node(encodedUrl);
      String url = EncodingUtils.decodeSlashes(encodedUrl);
      try {
        String username = serverNode.get("username", "");
        String password = serverNode.get("password", "");
        servers.put(url, new Host(url, username, password));
      } catch (StorageException e) {
        SonarLogger.log(e);
      }
    }
  }

  public void addServer(String location, String username, String password) throws Exception {
    if (useSecureStorage) {
      SonarServer sonarServer = new SonarServer(location);
      sonarServer.setCredentials(username, password);
    }
    servers.put(location, new Host(location, username, password));
  }

  private ISecurePreferences getSecurePreferences() {
    return SecurePreferencesFactory.getDefault().node(ISonarConstants.PLUGIN_ID);
  }

  public List<Host> getServers() {
    return Lists.newArrayList(servers.values());
  }

  public boolean removeServer(String host) {
    servers.remove(host);
    if (useSecureStorage) {
      ISecurePreferences securePreferences = getSecurePreferences();
      String encodedUrl = EncodingUtils.encodeSlashes(host);
      if (securePreferences.nodeExists(encodedUrl)) {
        securePreferences.node(encodedUrl).removeNode();
      }
    }
    return true;
  }

  /**
   * @deprecated since 0.3 use {@link #findServer(String)} instead
   */
  @Deprecated
  public Host createServer(String url) {
    return findServer(url);
  }

  public Host findServer(String host) {
    Host result = servers.get(host);
    if (result == null) {
      result = new Host(host, "", "");
      servers.put(host, result);
    }
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
