/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
 * sonarlint@sonarsource.com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.core.internal.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedGlobalConfiguration;

public class ServersManager {
  static final String PREF_SERVERS = "servers";
  private static final String INITIALIZED_ATTRIBUTE = "initialized";
  private static final String AUTH_ATTRIBUTE = "auth";
  private static final String URL_ATTRIBUTE = "url";
  private static final String USERNAME_ATTRIBUTE = "username";
  private static final String PASSWORD_ATTRIBUTE = "password";

  private static final byte EVENT_ADDED = 0;
  private static final byte EVENT_CHANGED = 1;
  private static final byte EVENT_REMOVED = 2;

  private static ServersManager instance = new ServersManager();

  private final Map<String, IServer> serversById = new LinkedHashMap<>();

  private final List<IServerLifecycleListener> serverListeners = new ArrayList<>();

  // resource change listeners
  private IResourceChangeListener resourceChangeListener;
  protected boolean ignorePreferenceChanges = false;

  private static boolean initialized;
  private static boolean initializing;

  /**
   * Cannot directly create a ResourceManager. Use
   * ServersCore.getResourceManager().
   */
  private ServersManager() {
    super();
  }

  protected synchronized void init() {
    if (initialized || initializing) {
      return;
    }

    initializing = true;

    loadServersList();

    initialized = true;
  }

  public static ServersManager getInstance() {
    return instance;
  }

  public static void shutdown() {
    if (instance == null) {
      return;
    }

    instance.shutdownImpl();
  }

  protected void shutdownImpl() {
    if (!initialized) {
      return;
    }

    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    if (workspace != null && resourceChangeListener != null) {
      workspace.removeResourceChangeListener(resourceChangeListener);
    }

  }

  public void addServerLifecycleListener(IServerLifecycleListener listener) {
    synchronized (serverListeners) {
      serverListeners.add(listener);
    }
  }

  public void removeServerLifecycleListener(IServerLifecycleListener listener) {
    synchronized (serverListeners) {
      serverListeners.remove(listener);
    }
  }

  /**
   * Fire a server event.
   */
  private void fireServerEvent(final IServer server, byte b) {

    if (serverListeners.isEmpty()) {
      return;
    }

    List<IServerLifecycleListener> clone = new ArrayList<>();
    clone.addAll(serverListeners);
    for (IServerLifecycleListener srl : clone) {
      if (b == EVENT_ADDED) {
        srl.serverAdded(server);
      } else if (b == EVENT_CHANGED) {
        srl.serverChanged(server);
      } else {
        srl.serverRemoved(server);
      }
    }
  }

  private void saveServersList() {
    try {
      IEclipsePreferences rootNode = InstanceScope.INSTANCE.getNode(SonarLintCorePlugin.PLUGIN_ID);
      rootNode.sync();
      Preferences serversNode = rootNode.node(PREF_SERVERS);
      serversNode.removeNode();
      serversNode = rootNode.node(PREF_SERVERS);
      serversNode.put(INITIALIZED_ATTRIBUTE, "true");
      for (IServer server : serversById.values()) {
        Preferences serverNode = serversNode.node(server.getId());
        serverNode.put(URL_ATTRIBUTE, server.getHost());
        serverNode.putBoolean(AUTH_ATTRIBUTE, server.hasAuth());
      }
      serversNode.flush();
    } catch (BackingStoreException e) {
      throw new IllegalStateException("Unable to save server list", e);
    }
  }

  protected void loadServersList() {
    try {
      IEclipsePreferences rootNode = InstanceScope.INSTANCE.getNode(SonarLintCorePlugin.PLUGIN_ID);
      rootNode.sync();
      Preferences serversNode = rootNode.nodeExists(PREF_SERVERS) ? rootNode.node(PREF_SERVERS) : DefaultScope.INSTANCE.getNode(SonarLintCorePlugin.PLUGIN_ID).node(PREF_SERVERS);
      for (String serverId : serversNode.childrenNames()) {
        Preferences serverNode = serversNode.node(serverId);
        boolean auth = serverNode.getBoolean(AUTH_ATTRIBUTE, false);
        String url = serverNode.get(URL_ATTRIBUTE, "");
        Server sonarServer = new Server(serverId, url, auth);
        serversById.put(serverId, sonarServer);
      }
    } catch (BackingStoreException e) {
      throw new IllegalStateException("Unable to load server list", e);
    }
  }

  public void addServer(IServer server, String username, String password) {
    if (!initialized) {
      init();
    }

    if (serversById.containsKey(server.getId())) {
      throw new IllegalStateException("There is already a server with id '" + server.getId() + "'");
    }
    serversById.put(server.getId(), server);
    saveServersList();
    if (server.hasAuth()) {
      storeCredentials(server, username, password);
    }
    fireServerEvent(server, EVENT_ADDED);
  }

  private static void storeCredentials(IServer server, String username, String password) {
    try {
      ISecurePreferences secureServersNode = SecurePreferencesFactory.getDefault().node(SonarLintCorePlugin.PLUGIN_ID).node(ServersManager.PREF_SERVERS);
      ISecurePreferences secureServerNode = secureServersNode.node(server.getId());
      secureServerNode.put(USERNAME_ATTRIBUTE, username, true);
      secureServerNode.put(PASSWORD_ATTRIBUTE, password, true);
      secureServersNode.flush();
    } catch (StorageException | IOException e) {
      throw new IllegalStateException("Unable to save secure credentials", e);
    }
  }

  public void removeServer(IServer server) {
    if (!initialized) {
      init();
    }

    if (serversById.containsKey(server.getId())) {
      serversById.remove(server.getId());
      saveServersList();
      ISecurePreferences secureServersNode = SecurePreferencesFactory.getDefault().node(SonarLintCorePlugin.PLUGIN_ID).node(ServersManager.PREF_SERVERS);
      secureServersNode.node(server.getId()).removeNode();
      fireServerEvent(server, EVENT_REMOVED);
    }
  }

  /**
   * Returns an array containing all servers.
   *
   * @return an array containing all servers
   */
  public List<IServer> getServers() {
    if (!initialized) {
      init();
    }

    return getServersNoInit();
  }

  public List<IServer> getServersNoInit() {
    return Collections.unmodifiableList(new ArrayList<>(serversById.values()));
  }

  /**
   * Returns the server with the given id.
   * 
   * @param id a server id
   * @return a server
   */
  public IServer getServer(String id) {
    if (id == null) {
      return null;
    }

    if (!initialized) {
      init();
    }

    return serversById.get(id);
  }

  public void updateServer(IServer server, String username, String password) {
    if (server == null) {
      return;
    }

    if (!initialized) {
      init();
    }

    if (!serversById.containsKey(server.getId())) {
      throw new IllegalStateException("There is no server with id '" + server.getId() + "'");
    }

    ((Server) serversById.get(server.getId())).stop();
    serversById.put(server.getId(), server);
    saveServersList();
    if (server.hasAuth()) {
      storeCredentials(server, username, password);
    }
    fireServerEvent(server, EVENT_CHANGED);
  }

  public static String getUsername(IServer server) {
    return getFromSecure(server, USERNAME_ATTRIBUTE);
  }

  public static String getPassword(IServer server) {
    return getFromSecure(server, PASSWORD_ATTRIBUTE);
  }

  private static String getFromSecure(IServer server, String attribute) {
    try {
      ISecurePreferences secureServersNode = SecurePreferencesFactory.getDefault().node(SonarLintCorePlugin.PLUGIN_ID).node(ServersManager.PREF_SERVERS);
      if (!secureServersNode.nodeExists(server.getId())) {
        return null;
      }
      ISecurePreferences secureServerNode = secureServersNode.node(server.getId());
      return secureServerNode.get(attribute, null);
    } catch (StorageException e) {
      throw new IllegalStateException("Unable to read secure credentials", e);
    }
  }

  public String validate(String serverId, String serverUrl, boolean editExisting) {
    if (StringUtils.isBlank(serverUrl)) {
      return "Server url must be specified";
    }
    if (StringUtils.isBlank(serverId)) {
      return "Server id must be specified";
    }
    if (!editExisting && serversById.containsKey(serverId)) {
      return "Server id already exists";
    }

    try {
      // Validate server ID format
      ConnectedGlobalConfiguration.builder()
        .setServerId(serverId)
        .build();
    } catch (Exception e) {
      return e.getMessage();
    }
    return null;
  }

  public IServer create(String id, String url, String username, String password) {
    return new Server(id, url, StringUtils.isNotBlank(username) || StringUtils.isNotBlank(password));
  }

}
