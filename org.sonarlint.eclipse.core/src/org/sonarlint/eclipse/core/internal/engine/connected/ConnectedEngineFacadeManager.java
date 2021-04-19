/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2021 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.engine.connected;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.INodeChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.NodeChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jdt.annotation.Nullable;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedGlobalConfiguration;

public class ConnectedEngineFacadeManager {
  public static final String PREF_SERVERS = "servers";
  static final String AUTH_ATTRIBUTE = "auth";
  static final String URL_ATTRIBUTE = "url";
  static final String ORG_ATTRIBUTE = "org";
  static final String USERNAME_ATTRIBUTE = "username";
  static final String PASSWORD_ATTRIBUTE = "password";
  static final String NOTIFICATIONS_DISABLED_ATTRIBUTE = "notificationsDisabled";

  private static final byte EVENT_ADDED = 0;
  private static final byte EVENT_CHANGED = 1;
  private static final byte EVENT_REMOVED = 2;

  private final Map<String, IConnectedEngineFacade> facadesByConnectionId = new LinkedHashMap<>();

  private final List<IConnectedEngineFacadeLifecycleListener> connectionsListeners = new ArrayList<>();

  private final IPreferenceChangeListener connectedEngineChangeListener = event -> {
    try {
      if (!event.getNode().nodeExists("") || !event.getNode().parent().nodeExists("")) {
        // Deletion in progress
        return;
      }
    } catch (BackingStoreException e) {
      throw new IllegalStateException(e);
    }
    String connectionId = getConnectionIdFromNodeName(event.getNode().name());
    ConnectedEngineFacade old = (ConnectedEngineFacade) facadesByConnectionId.get(connectionId);
    if (old != null) {
      loadConnection(event.getNode(), old);
      old.stop();
      fireServerEvent(old, EVENT_CHANGED);
    } else {
      ConnectedEngineFacade newFacade = new ConnectedEngineFacade(connectionId);
      loadConnection(event.getNode(), newFacade);
      facadesByConnectionId.put(newFacade.getId(), newFacade);
      fireServerEvent(newFacade, EVENT_ADDED);
    }
  };

  private final INodeChangeListener serversNodeChangeListener = new INodeChangeListener() {

    @Override
    public void removed(NodeChangeEvent event) {
      String serverId = getConnectionIdFromNodeName(event.getChild().name());
      IConnectedEngineFacade serverToRemove = facadesByConnectionId.get(serverId);
      facadesByConnectionId.remove(serverId);
      if (serverToRemove != null) {
        fireServerEvent(serverToRemove, EVENT_REMOVED);
      }
    }

    @Override
    public void added(NodeChangeEvent event) {
      ((IEclipsePreferences) event.getChild()).addPreferenceChangeListener(connectedEngineChangeListener);
    }
  };

  private final INodeChangeListener rootNodeChangeListener = new INodeChangeListener() {

    @Override
    public void removed(NodeChangeEvent event) {
      if (event.getChild().name().equals(PREF_SERVERS)) {
        Collection<IConnectedEngineFacade> removedServers = new ArrayList<>(facadesByConnectionId.values());
        facadesByConnectionId.clear();
        for (IConnectedEngineFacade server : removedServers) {
          fireServerEvent(server, EVENT_REMOVED);
        }
        // Reload default values
        facadesByConnectionId.putAll(loadServersList(DefaultScope.INSTANCE.getNode(SonarLintCorePlugin.PLUGIN_ID).node(PREF_SERVERS)));
        for (IConnectedEngineFacade server : facadesByConnectionId.values()) {
          fireServerEvent(server, EVENT_ADDED);
        }
      }
    }

    @Override
    public void added(NodeChangeEvent event) {
      if (event.getChild().name().equals(PREF_SERVERS)) {
        // Default servers
        Collection<IConnectedEngineFacade> removedServers = new ArrayList<>(facadesByConnectionId.values());
        facadesByConnectionId.clear();
        for (IConnectedEngineFacade server : removedServers) {
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
        facadesByConnectionId.putAll(loadServersList(serversNode));
        for (String serverNodeName : serversNode.childrenNames()) {
          IEclipsePreferences serverNode = ((IEclipsePreferences) serversNode.node(serverNodeName));
          serverNode.addPreferenceChangeListener(connectedEngineChangeListener);
        }
      } else {
        facadesByConnectionId.putAll(loadServersList(DefaultScope.INSTANCE.getNode(SonarLintCorePlugin.PLUGIN_ID).node(PREF_SERVERS)));
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
          serverNode.removePreferenceChangeListener(connectedEngineChangeListener);
        }
      }
    } catch (BackingStoreException e) {
      throw unableToLoadServerList(e);
    }
    connectionsListeners.clear();
  }

  public void addServerLifecycleListener(IConnectedEngineFacadeLifecycleListener listener) {
    synchronized (connectionsListeners) {
      connectionsListeners.add(listener);
    }
  }

  public void removeServerLifecycleListener(IConnectedEngineFacadeLifecycleListener listener) {
    synchronized (connectionsListeners) {
      connectionsListeners.remove(listener);
    }
  }

  private void fireServerEvent(final IConnectedEngineFacade server, byte b) {

    if (connectionsListeners.isEmpty()) {
      return;
    }

    List<IConnectedEngineFacadeLifecycleListener> clone = new ArrayList<>();
    clone.addAll(connectionsListeners);
    for (IConnectedEngineFacadeLifecycleListener srl : clone) {
      if (b == EVENT_ADDED) {
        srl.connectionAdded(server);
      } else if (b == EVENT_CHANGED) {
        srl.connectionChanged(server);
      } else {
        srl.connectionRemoved(server);
      }
    }
  }

  private static IEclipsePreferences getSonarLintPreferenceNode() {
    return InstanceScope.INSTANCE.getNode(SonarLintCorePlugin.PLUGIN_ID);
  }

  private static Map<String, IConnectedEngineFacade> loadServersList(Preferences serversNode) {
    Map<String, IConnectedEngineFacade> result = new LinkedHashMap<>();
    try {
      for (String serverNodeName : serversNode.childrenNames()) {
        Preferences serverNode = serversNode.node(serverNodeName);
        String serverId = getConnectionIdFromNodeName(serverNodeName);
        ConnectedEngineFacade s = new ConnectedEngineFacade(serverId);
        loadConnection(serverNode, s);
        result.put(s.getId(), s);
      }
    } catch (BackingStoreException e) {
      throw unableToLoadServerList(e);
    }
    return result;
  }

  private static IllegalStateException unableToLoadServerList(BackingStoreException e) {
    return new IllegalStateException("Unable to load connections list", e);
  }

  private static void loadConnection(Preferences connectionNode, ConnectedEngineFacade facade) {
    String url = connectionNode.get(URL_ATTRIBUTE, "");
    url = StringUtils.removeEnd(url, "/");
    if (ConnectedEngineFacade.OLD_SONARCLOUD_URL.equals(url)) {
      // Migration
      url = ConnectedEngineFacade.getSonarCloudUrl();
      connectionNode.put(URL_ATTRIBUTE, url);
    }
    update(facade,
      url,
      connectionNode.get(ORG_ATTRIBUTE, null),
      connectionNode.getBoolean(AUTH_ATTRIBUTE, false),
      connectionNode.getBoolean(NOTIFICATIONS_DISABLED_ATTRIBUTE, false));
  }

  private static String getConnectionIdFromNodeName(String name) {
    return StringUtils.urlDecode(name);
  }

  public void addServer(IConnectedEngineFacade facade, String username, String password) {
    if (facadesByConnectionId.containsKey(facade.getId())) {
      throw new IllegalStateException("There is already a connection with id '" + facade.getId() + "'");
    }
    if (hasAuth(username, password)) {
      storeCredentials(facade, username, password);
    }
    addOrUpdateProperties(facade);
    facadesByConnectionId.put(facade.getId(), facade);
    fireServerEvent(facade, EVENT_ADDED);
  }

  private static void storeCredentials(IConnectedEngineFacade server, String username, String password) {
    try {
      ISecurePreferences secureConnectionsNode = getSecureConnectionsNode();
      ISecurePreferences secureConnectionNode = secureConnectionsNode.node(getConnectionNodeName(server));
      secureConnectionNode.put(USERNAME_ATTRIBUTE, username, true);
      secureConnectionNode.put(PASSWORD_ATTRIBUTE, password, true);
      secureConnectionsNode.flush();
    } catch (StorageException | IOException e) {
      throw new IllegalStateException("Unable to store connection credentials in secure storage: " + e.getMessage(), e);
    }
  }

  public void removeServer(IConnectedEngineFacade server) {
    String serverNodeName = getConnectionNodeName(server);
    try {
      IEclipsePreferences rootNode = getSonarLintPreferenceNode();
      Preferences serversNode = rootNode.node(PREF_SERVERS);
      if (serversNode.nodeExists(serverNodeName)) {
        // No need to notify listener for every deleted property
        ((IEclipsePreferences) serversNode.node(serverNodeName)).removePreferenceChangeListener(connectedEngineChangeListener);
        serversNode.node(serverNodeName).removeNode();
        serversNode.flush();
      }
    } catch (BackingStoreException e) {
      throw new IllegalStateException("Unable to save server list", e);
    }
    tryRemoveSecureProperties(serverNodeName);
  }

  private static void tryRemoveSecureProperties(String serverNodeName) {
    ISecurePreferences secureServersNode = getSecureConnectionsNode();
    if (secureServersNode.nodeExists(serverNodeName)) {
      secureServersNode.node(serverNodeName).removeNode();
    }
  }

  /**
   * Returns an array containing all servers.
   *
   * @return an array containing all servers
   */
  public List<IConnectedEngineFacade> getServers() {
    return Collections.unmodifiableList(new ArrayList<>(facadesByConnectionId.values()));
  }

  /**
   * Returns the server with the given id.
   *
   * @param id a server id
   * @return a server or empty
   */
  public Optional<IConnectedEngineFacade> findById(String id) {
    return Optional.ofNullable(facadesByConnectionId.get(Objects.requireNonNull(id)));
  }

  public List<IConnectedEngineFacade> findByUrl(String serverUrl) {
    return facadesByConnectionId.values().stream()
      .filter(facade -> equalsIgnoringTrailingSlash(facade.getHost(), serverUrl))
      .collect(Collectors.toList());
  }

  private static boolean equalsIgnoringTrailingSlash(String aUrl, String anotherUrl) {
    try {
      return new URI(StringUtils.removeEnd(aUrl, "/")).equals(new URI(StringUtils.removeEnd(anotherUrl, "/")));
    } catch (URISyntaxException e) {
      // should never happen at this stage
      SonarLintLogger.get().error("Malformed server URL", e);
      return false;
    }
  }

  public Optional<ResolvedBinding> resolveBinding(ISonarLintProject project) {
    return resolveBinding(project, SonarLintCorePlugin.loadConfig(project));
  }

  public Optional<ResolvedBinding> resolveBinding(ISonarLintProject project, SonarLintProjectConfiguration config) {
    return config
      .getProjectBinding()
      .flatMap(b -> {
        Optional<IConnectedEngineFacade> server = findById(b.connectionId());
        if (!server.isPresent()) {
          SonarLintLogger.get().error("Project '" + project.getName() + "' binding refers to an unknown connection: '" + b.connectionId()
            + "'. Please fix project binding or unbind project.");
          return Optional.empty();
        }
        return Optional.of(new ResolvedBinding(config.getProjectBinding().get(), server.get()));
      });
  }

  public void updateConnection(IConnectedEngineFacade facade, String username, String password) {
    if (!facadesByConnectionId.containsKey(facade.getId())) {
      throw new IllegalStateException("There is no connection with id '" + facade.getId() + "'");
    }

    IEclipsePreferences rootNode = getSonarLintPreferenceNode();
    try {
      if (!rootNode.nodeExists(PREF_SERVERS)) {
        // User is probably editing a default server. So we have to make them persistent.
        Collection<IConnectedEngineFacade> defaultServers = new ArrayList<>(facadesByConnectionId.values());
        facadesByConnectionId.clear();
        for (IConnectedEngineFacade iServer : defaultServers) {
          addServer(iServer, "", "");
        }
      }
    } catch (BackingStoreException e) {
      throw new IllegalStateException("Unable to save connection", e);
    }

    addOrUpdateProperties(facade);
    if (facade.hasAuth()) {
      storeCredentials(facade, username, password);
    }
    ConnectedEngineFacade connectionToUpdate = (ConnectedEngineFacade) facadesByConnectionId.get(facade.getId());
    update(connectionToUpdate, facade.getHost(), facade.getOrganization(), facade.hasAuth(), facade.areNotificationsDisabled());

    fireServerEvent(connectionToUpdate, EVENT_CHANGED);
  }

  private void addOrUpdateProperties(IConnectedEngineFacade facade) {
    IEclipsePreferences rootNode = getSonarLintPreferenceNode();
    Preferences serversNode = rootNode.node(PREF_SERVERS);
    IEclipsePreferences serverNode = (IEclipsePreferences) serversNode.node(getConnectionNodeName(facade));
    try {
      serverNode.removePreferenceChangeListener(connectedEngineChangeListener);
      serverNode.put(URL_ATTRIBUTE, facade.getHost());
      if (StringUtils.isNotBlank(facade.getOrganization())) {
        serverNode.put(ORG_ATTRIBUTE, facade.getOrganization());
      } else {
        serverNode.remove(ORG_ATTRIBUTE);
      }
      serverNode.putBoolean(AUTH_ATTRIBUTE, facade.hasAuth());
      if (facade.areNotificationsDisabled()) {
        serverNode.putBoolean(NOTIFICATIONS_DISABLED_ATTRIBUTE, true);
      } else {
        serverNode.remove(NOTIFICATIONS_DISABLED_ATTRIBUTE);
      }
      serversNode.flush();
    } catch (BackingStoreException e) {
      throw new IllegalStateException("Unable to save server list", e);
    } finally {
      serverNode.addPreferenceChangeListener(connectedEngineChangeListener);
    }
  }

  @Nullable
  public static String getUsername(IConnectedEngineFacade facade) throws StorageException {
    return getFromSecure(facade, USERNAME_ATTRIBUTE);
  }

  @Nullable
  public static String getPassword(IConnectedEngineFacade facade) throws StorageException {
    return getFromSecure(facade, PASSWORD_ATTRIBUTE);
  }

  @Nullable
  private static String getFromSecure(IConnectedEngineFacade facade, String attribute) throws StorageException {
    ISecurePreferences secureConnectionsNode = getSecureConnectionsNode();
    if (!secureConnectionsNode.nodeExists(getConnectionNodeName(facade))) {
      return null;
    }
    ISecurePreferences secureConnectionNode = secureConnectionsNode.node(getConnectionNodeName(facade));
    return secureConnectionNode.get(attribute, null);
  }

  private static ISecurePreferences getSecureConnectionsNode() {
    return SecurePreferencesFactory.getDefault().node(SonarLintCorePlugin.PLUGIN_ID).node(ConnectedEngineFacadeManager.PREF_SERVERS);
  }

  private static String getConnectionNodeName(IConnectedEngineFacade facade) {
    // Should not contain any "/"
    return StringUtils.urlEncode(facade.getId());
  }

  @Nullable
  public String validate(String connectionId, boolean editExisting) {
    if (StringUtils.isBlank(connectionId)) {
      return "Connection name must be specified";
    }
    if (!editExisting && facadesByConnectionId.containsKey(connectionId)) {
      return "Connection name already exists";
    }

    try {
      // Validate connection ID format
      ConnectedGlobalConfiguration.builder()
        .setConnectionId(connectionId)
        .build();
    } catch (Exception e) {
      return e.getMessage();
    }
    return null;
  }

  public IConnectedEngineFacade create(String id, String url, @Nullable String organization, String username, String password, boolean notificationsEnabled) {
    return update(new ConnectedEngineFacade(id), url, organization, hasAuth(username, password), notificationsEnabled);
  }

  private static boolean hasAuth(@Nullable String username, @Nullable String password) {
    return StringUtils.isNotBlank(username) || StringUtils.isNotBlank(password);
  }

  private static ConnectedEngineFacade update(ConnectedEngineFacade facade, String url, @Nullable String organization, boolean hasAuth, boolean notificationsDisabled) {
    return facade.setHost(url)
      .setOrganization(organization)
      .setHasAuth(hasAuth)
      .setNotificationsDisabled(notificationsDisabled);
  }

}
