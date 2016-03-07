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
import java.util.List;
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

public class ServersManager {
  static final String PREF_SERVERS = "servers";
  private static final String INITIALIZED_ATTRIBUTE = "initialized";
  private static final String NAME_ATTRIBUTE = "name";
  private static final String AUTH_ATTRIBUTE = "auth";
  private static final String URL_ATTRIBUTE = "url";
  private static final String USERNAME_ATTRIBUTE = "username";
  private static final String PASSWORD_ATTRIBUTE = "password";

  private static final byte EVENT_ADDED = 0;
  private static final byte EVENT_CHANGED = 1;
  private static final byte EVENT_REMOVED = 2;

  private static ServersManager instance = new ServersManager();

  protected List<IServer> servers;

  protected List<IServerLifecycleListener> serverListeners = new ArrayList<>(3);

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
    if (initialized || initializing)
      return;

    initializing = true;

    servers = new ArrayList<IServer>();

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
   * Deregister an existing server resource.
   *
   * @param server
   */
  private void deregisterServer(IServer server) {
    if (server == null) {
      return;
    }

    servers.remove(server);
    fireServerEvent(server, EVENT_REMOVED);
  }

  /**
   * Fire a server event.
   */
  private void fireServerEvent(final IServer server, byte b) {

    if (serverListeners.isEmpty()) {
      return;
    }

    List<IServerLifecycleListener> clone = new ArrayList<IServerLifecycleListener>();
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
      ISecurePreferences secureServersNode = null;
      rootNode.sync();
      Preferences serversNode = rootNode.node(PREF_SERVERS);
      serversNode.removeNode();
      serversNode = rootNode.node(PREF_SERVERS);
      serversNode.put(INITIALIZED_ATTRIBUTE, "true");
      for (IServer server : servers) {
        Preferences serverNode = serversNode.node(server.getId());
        serverNode.put(NAME_ATTRIBUTE, server.getName());
        serverNode.put(URL_ATTRIBUTE, server.getHost());
        serverNode.putBoolean(AUTH_ATTRIBUTE, server.hasAuth());
        if (server.hasAuth()) {
          secureServersNode = secureServersNode != null ? secureServersNode
            : SecurePreferencesFactory.getDefault().node(SonarLintCorePlugin.PLUGIN_ID).node(ServersManager.PREF_SERVERS);
          ISecurePreferences secureServerNode = secureServersNode.node(server.getId());
          secureServerNode.put(USERNAME_ATTRIBUTE, server.getUsername(), true);
          secureServerNode.put(PASSWORD_ATTRIBUTE, server.getPassword(), true);
        }
      }
      serversNode.flush();
      if (secureServersNode != null) {
        secureServersNode.flush();
      }
    } catch (IOException | StorageException | BackingStoreException e) {
      throw new IllegalStateException("Unable to save server list", e);
    }
  }

  protected void loadServersList() {
    try {
      IEclipsePreferences rootNode = InstanceScope.INSTANCE.getNode(SonarLintCorePlugin.PLUGIN_ID);
      ISecurePreferences secureServersNode = null;
      rootNode.sync();
      Preferences serversNode = rootNode.nodeExists(PREF_SERVERS) ? rootNode.node(PREF_SERVERS) : DefaultScope.INSTANCE.getNode(SonarLintCorePlugin.PLUGIN_ID).node(PREF_SERVERS);
      for (String serverId : serversNode.childrenNames()) {
        Preferences serverNode = serversNode.node(serverId);
        boolean auth = serverNode.getBoolean(AUTH_ATTRIBUTE, false);
        String name = serverNode.get(NAME_ATTRIBUTE, "");
        String url = serverNode.get(URL_ATTRIBUTE, "");
        String username = "";
        String password = "";
        if (auth) {
          secureServersNode = secureServersNode != null ? secureServersNode
            : SecurePreferencesFactory.getDefault().node(SonarLintCorePlugin.PLUGIN_ID).node(ServersManager.PREF_SERVERS);
          ISecurePreferences secureServerNode = secureServersNode.node(serverId);
          username = secureServerNode.get(USERNAME_ATTRIBUTE, "");
          password = secureServerNode.get(PASSWORD_ATTRIBUTE, "");
        }
        Server sonarServer = new Server(serverId, name, url, username, password);
        servers.add(sonarServer);
      }
    } catch (StorageException | BackingStoreException e) {
      throw new IllegalStateException("Unable to load server list", e);
    }
  }

  public void addServer(IServer server) {
    if (server == null)
      return;

    if (!initialized)
      init();

    if (!servers.contains(server))
      registerServer(server);
    else
      fireServerEvent(server, EVENT_CHANGED);
    saveServersList();
  }

  public void removeServer(IServer server) {
    if (!initialized) {
      init();
    }

    if (servers.contains(server)) {
      deregisterServer(server);
      saveServersList();
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

    return Collections.unmodifiableList(servers);
  }

  /**
   * Returns the server with the given id.
   * 
   * @param id a server id
   * @return a server
   */
  public IServer getServer(String id) {
    if (!initialized) {
      init();
    }

    if (id == null) {
      throw new IllegalArgumentException();
    }

    for (IServer server : servers) {
      if (id.equals(server.getId())) {
        return server;
      }
    }
    return null;
  }

  /**
   * Registers a new server.
   *
   * @param server a server
   */
  private void registerServer(IServer server) {
    if (server == null) {
      return;
    }

    servers.add(server);
    fireServerEvent(server, EVENT_ADDED);
  }

}
