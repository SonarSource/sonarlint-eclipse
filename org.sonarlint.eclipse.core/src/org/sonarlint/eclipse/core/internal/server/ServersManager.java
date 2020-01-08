/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2020 SonarSource SA
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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.INodeChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.NodeChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedGlobalConfiguration;

public class ServersManager {
  static final String PREF_SERVERS = "servers";
  static final String AUTH_ATTRIBUTE = "auth";
  static final String URL_ATTRIBUTE = "url";
  static final String ORG_ATTRIBUTE = "org";
  static final String USERNAME_ATTRIBUTE = "username";
  static final String PASSWORD_ATTRIBUTE = "password";
  static final String NOTIFICATIONS_ENABLED_ATTRIBUTE = "notificationsEnabled";

  private static final byte EVENT_ADDED = 0;
  private static final byte EVENT_CHANGED = 1;
  private static final byte EVENT_REMOVED = 2;

  private final Map<String, IServer> serversById = new LinkedHashMap<>();

  private final List<IServerLifecycleListener> serverListeners = new ArrayList<>();

  private final IPreferenceChangeListener serverChangeListener = event -> {
    try {
      if (!event.getNode().nodeExists("") || !event.getNode().parent().nodeExists("")) {
        // Deletion in progress
        return;
      }
    } catch (BackingStoreException e) {
      throw new IllegalStateException(e);
    }
    String serverId = getServerIdFromNodeName(event.getNode().name());
    Server old = (Server) serversById.get(serverId);
    if (old != null) {
      loadServer(event.getNode(), old);
      old.stop();
      fireServerEvent(old, EVENT_CHANGED);
    } else {
      Server newServer = new Server(serverId);
      loadServer(event.getNode(), newServer);
      serversById.put(newServer.getId(), newServer);
      fireServerEvent(newServer, EVENT_ADDED);
    }
  };

  private final INodeChangeListener serversNodeChangeListener = new INodeChangeListener() {

    @Override
    public void removed(NodeChangeEvent event) {
      String serverId = getServerIdFromNodeName(event.getChild().name());
      IServer serverToRemove = serversById.get(serverId);
      serversById.remove(serverId);
      if (serverToRemove != null) {
        fireServerEvent(serverToRemove, EVENT_REMOVED);
      }
    }

    @Override
    public void added(NodeChangeEvent event) {
      ((IEclipsePreferences) event.getChild()).addPreferenceChangeListener(serverChangeListener);
    }
  };

  private final INodeChangeListener rootNodeChangeListener = new INodeChangeListener() {

    @Override
    public void removed(NodeChangeEvent event) {
      if (event.getChild().name().equals(PREF_SERVERS)) {
        Collection<IServer> removedServers = new ArrayList<>(serversById.values());
        serversById.clear();
        for (IServer server : removedServers) {
          fireServerEvent(server, EVENT_REMOVED);
        }
        // Reload default values
        serversById.putAll(loadServersList(DefaultScope.INSTANCE.getNode(SonarLintCorePlugin.PLUGIN_ID).node(PREF_SERVERS)));
        for (IServer server : serversById.values()) {
          fireServerEvent(server, EVENT_ADDED);
        }
      }
    }

    @Override
    public void added(NodeChangeEvent event) {
      if (event.getChild().name().equals(PREF_SERVERS)) {
        // Default servers
        Collection<IServer> removedServers = new ArrayList<>(serversById.values());
        serversById.clear();
        for (IServer server : removedServers) {
          fireServerEvent(server, EVENT_REMOVED);
        }
        ((IEclipsePreferences) event.getChild()).addNodeChangeListener(serversNodeChangeListener);
      }
    }
  };

  public void init() {
    IEclipsePreferences rootNode = getSonarLintPreferenceNode();
    rootNode.addNodeChangeListener(rootNodeChangeListener);
    try {
      if (rootNode.nodeExists(PREF_SERVERS)) {
        Preferences serversNode = rootNode.node(PREF_SERVERS);
        ((IEclipsePreferences) serversNode).addNodeChangeListener(serversNodeChangeListener);
        serversById.putAll(loadServersList(serversNode));
        for (String serverNodeName : serversNode.childrenNames()) {
          IEclipsePreferences serverNode = ((IEclipsePreferences) serversNode.node(serverNodeName));
          serverNode.addPreferenceChangeListener(serverChangeListener);
        }
      } else {
        serversById.putAll(loadServersList(DefaultScope.INSTANCE.getNode(SonarLintCorePlugin.PLUGIN_ID).node(PREF_SERVERS)));
      }
    } catch (BackingStoreException e) {
      throw unableToLoadServerList(e);
    }
  }

  public void stop() {
    IEclipsePreferences rootNode = getSonarLintPreferenceNode();
    rootNode.removeNodeChangeListener(rootNodeChangeListener);
    try {
      if (rootNode.nodeExists(PREF_SERVERS)) {
        Preferences serversNode = rootNode.node(PREF_SERVERS);
        ((IEclipsePreferences) serversNode).removeNodeChangeListener(serversNodeChangeListener);
        for (String serverNodeName : serversNode.childrenNames()) {
          IEclipsePreferences serverNode = ((IEclipsePreferences) serversNode.node(serverNodeName));
          serverNode.removePreferenceChangeListener(serverChangeListener);
        }
      }
    } catch (BackingStoreException e) {
      throw unableToLoadServerList(e);
    }
    serverListeners.clear();
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

  private static IEclipsePreferences getSonarLintPreferenceNode() {
    return InstanceScope.INSTANCE.getNode(SonarLintCorePlugin.PLUGIN_ID);
  }

  private static Map<String, IServer> loadServersList(Preferences serversNode) {
    Map<String, IServer> result = new LinkedHashMap<>();
    try {
      for (String serverNodeName : serversNode.childrenNames()) {
        Preferences serverNode = serversNode.node(serverNodeName);
        String serverId = getServerIdFromNodeName(serverNodeName);
        Server s = new Server(serverId);
        loadServer(serverNode, s);
        result.put(s.getId(), s);
      }
    } catch (BackingStoreException e) {
      throw unableToLoadServerList(e);
    }
    return result;
  }

  private static IllegalStateException unableToLoadServerList(BackingStoreException e) {
    return new IllegalStateException("Unable to load server list", e);
  }

  private static void loadServer(Preferences serverNode, Server server) {
    String url = serverNode.get(URL_ATTRIBUTE, "");
    url = StringUtils.removeEnd(url, "/");
    if (Server.OLD_SONARCLOUD_URL.equals(url)) {
      // Migration
      url = Server.getSonarCloudUrl();
      serverNode.put(URL_ATTRIBUTE, url);
    }
    update(server,
      url,
      serverNode.get(ORG_ATTRIBUTE, null),
      serverNode.getBoolean(AUTH_ATTRIBUTE, false),
      serverNode.getBoolean(NOTIFICATIONS_ENABLED_ATTRIBUTE, false));
  }

  private static String getServerIdFromNodeName(String name) {
    return StringUtils.urlDecode(name);
  }

  public void addServer(IServer server, String username, String password) {
    if (serversById.containsKey(server.getId())) {
      throw new IllegalStateException("There is already a server with id '" + server.getId() + "'");
    }
    if (hasAuth(username, password)) {
      storeCredentials(server, username, password);
    }
    addOrUpdateProperties(server);
    serversById.put(server.getId(), server);
    fireServerEvent(server, EVENT_ADDED);
  }

  private static void storeCredentials(IServer server, String username, String password) {
    try {
      ISecurePreferences secureServersNode = getSecureServersNode();
      ISecurePreferences secureServerNode = secureServersNode.node(getServerNodeName(server));
      secureServerNode.put(USERNAME_ATTRIBUTE, username, true);
      secureServerNode.put(PASSWORD_ATTRIBUTE, password, true);
      secureServersNode.flush();
    } catch (StorageException | IOException e) {
      throw new IllegalStateException("Unable to store server credentials in secure storage: " + e.getMessage(), e);
    }
  }

  public void removeServer(IServer server) {
    String serverNodeName = getServerNodeName(server);
    try {
      IEclipsePreferences rootNode = getSonarLintPreferenceNode();
      Preferences serversNode = rootNode.node(PREF_SERVERS);
      if (serversNode.nodeExists(serverNodeName)) {
        // No need to notify listener for every deleted property
        ((IEclipsePreferences) serversNode.node(serverNodeName)).removePreferenceChangeListener(serverChangeListener);
        serversNode.node(serverNodeName).removeNode();
        serversNode.flush();
      }
    } catch (BackingStoreException e) {
      throw new IllegalStateException("Unable to save server list", e);
    }
    tryRemoveSecureProperties(serverNodeName);
  }

  private static void tryRemoveSecureProperties(String serverNodeName) {
    ISecurePreferences secureServersNode = getSecureServersNode();
    if (secureServersNode.nodeExists(serverNodeName)) {
      secureServersNode.node(serverNodeName).removeNode();
    }
  }

  /**
   * Returns an array containing all servers.
   *
   * @return an array containing all servers
   */
  public List<IServer> getServers() {
    return Collections.unmodifiableList(new ArrayList<>(serversById.values()));
  }

  /**
   * Returns the server with the given id.
   * 
   * @param id a server id
   * @return a server or empty
   */
  public Optional<IServer> findById(String id) {
    return Optional.ofNullable(serversById.get(Objects.requireNonNull(id)));
  }

  public Optional<IServer> forProject(ISonarLintProject project) {
    return forProject(project, SonarLintCorePlugin.loadConfig(project));
  }

  public Optional<IServer> forProject(ISonarLintProject project, SonarLintProjectConfiguration config) {
    return config
      .getProjectBinding()
      .flatMap(b -> {
        Optional<IServer> server = findById(b.serverId());
        if (!server.isPresent()) {
          SonarLintLogger.get().error("Project '" + project.getName() + "' is bound to an unknown server: '" + b.serverId()
            + "'. Please fix project binding or unbind project.");
          return Optional.empty();
        }
        return server;
      });
  }

  public void updateServer(IServer server, String username, String password) {
    if (!serversById.containsKey(server.getId())) {
      throw new IllegalStateException("There is no server with id '" + server.getId() + "'");
    }

    IEclipsePreferences rootNode = getSonarLintPreferenceNode();
    try {
      if (!rootNode.nodeExists(PREF_SERVERS)) {
        // User is probably editing a default server. So we have to make them persistent.
        Collection<IServer> defaultServers = new ArrayList<>(serversById.values());
        serversById.clear();
        for (IServer iServer : defaultServers) {
          addServer(iServer, "", "");
        }
      }
    } catch (BackingStoreException e) {
      throw new IllegalStateException("Unable to save server", e);
    }

    addOrUpdateProperties(server);
    if (server.hasAuth()) {
      storeCredentials(server, username, password);
    }
    Server serverToUpdate = (Server) serversById.get(server.getId());
    update(serverToUpdate, server.getHost(), server.getOrganization(), server.hasAuth(), server.areNotificationsEnabled());

    fireServerEvent(serverToUpdate, EVENT_CHANGED);
  }

  private void addOrUpdateProperties(IServer server) {
    IEclipsePreferences rootNode = getSonarLintPreferenceNode();
    Preferences serversNode = rootNode.node(PREF_SERVERS);
    IEclipsePreferences serverNode = (IEclipsePreferences) serversNode.node(getServerNodeName(server));
    try {
      serverNode.removePreferenceChangeListener(serverChangeListener);
      serverNode.put(URL_ATTRIBUTE, server.getHost());
      if (StringUtils.isNotBlank(server.getOrganization())) {
        serverNode.put(ORG_ATTRIBUTE, server.getOrganization());
      }
      serverNode.putBoolean(AUTH_ATTRIBUTE, server.hasAuth());
      serverNode.putBoolean(NOTIFICATIONS_ENABLED_ATTRIBUTE, server.areNotificationsEnabled());
      serversNode.flush();
    } catch (BackingStoreException e) {
      throw new IllegalStateException("Unable to save server list", e);
    } finally {
      serverNode.addPreferenceChangeListener(serverChangeListener);
    }
  }

  public static String getUsername(IServer server) throws StorageException {
    return getFromSecure(server, USERNAME_ATTRIBUTE);
  }

  public static String getPassword(IServer server) throws StorageException {
    return getFromSecure(server, PASSWORD_ATTRIBUTE);
  }

  private static String getFromSecure(IServer server, String attribute) throws StorageException {
    ISecurePreferences secureServersNode = getSecureServersNode();
    if (!secureServersNode.nodeExists(getServerNodeName(server))) {
      return null;
    }
    ISecurePreferences secureServerNode = secureServersNode.node(getServerNodeName(server));
    return secureServerNode.get(attribute, null);
  }

  private static ISecurePreferences getSecureServersNode() {
    return SecurePreferencesFactory.getDefault().node(SonarLintCorePlugin.PLUGIN_ID).node(ServersManager.PREF_SERVERS);
  }

  private static String getServerNodeName(IServer server) {
    // Should not contain any "/"
    return StringUtils.urlEncode(server.getId());
  }

  public String validate(String serverId, boolean editExisting) {
    if (StringUtils.isBlank(serverId)) {
      return "Connection name must be specified";
    }
    if (!editExisting && serversById.containsKey(serverId)) {
      return "Connection name already exists";
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

  public IServer create(String id, String url, @Nullable String organization, String username, String password, boolean notificationsEnabled) {
    return update(new Server(id), url, organization, hasAuth(username, password), notificationsEnabled);
  }

  private static boolean hasAuth(@Nullable String username, @Nullable String password) {
    return StringUtils.isNotBlank(username) || StringUtils.isNotBlank(password);
  }

  private static Server update(Server server, String url, @Nullable String organization, boolean hasAuth, boolean notificationsEnabled) {
    return server.setHost(url)
      .setOrganization(organization)
      .setHasAuth(hasAuth)
      .setNotificationsEnabled(notificationsEnabled);
  }

}
