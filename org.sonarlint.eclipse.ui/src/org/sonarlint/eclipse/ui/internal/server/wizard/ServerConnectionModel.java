/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2018 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.server.wizard;

import java.net.MalformedURLException;
import java.net.URL;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.core.internal.server.Server;
import org.sonarlint.eclipse.core.internal.server.ServersManager;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.ui.internal.util.wizard.ModelObject;
import org.sonarsource.sonarlint.core.client.api.connected.RemoteOrganization;
import org.sonarsource.sonarlint.core.client.api.util.TextSearchIndex;

public class ServerConnectionModel extends ModelObject {

  private static final String ERROR_READING_SECURE_STORAGE = "Error reading secure storage";

  public static final String PROPERTY_CONNECTION_TYPE = "connectionType";
  public static final String PROPERTY_SERVER_URL = "serverUrl";
  public static final String PROPERTY_AUTH_METHOD = "authMethod";
  public static final String PROPERTY_USERNAME = "username";
  public static final String PROPERTY_PASSWORD = "password";
  public static final String PROPERTY_ORGANIZATION = "organization";
  public static final String PROPERTY_SERVER_ID = "serverId";
  public static final String PROPERTY_NOTIFICATIONS_ENABLED = "notificationsEnabled";

  public enum ConnectionType {
    SONARCLOUD, ONPREMISE
  }

  public enum AuthMethod {
    TOKEN, PASSWORD
  }

  private final boolean edit;
  private ConnectionType connectionType = ConnectionType.SONARCLOUD;
  private AuthMethod authMethod = AuthMethod.TOKEN;
  private String serverId;
  private String serverUrl = Server.SONARCLOUD_URL;
  private String organization;
  private String username;
  private String password;
  private TextSearchIndex<RemoteOrganization> organizationsIndex;
  private boolean notificationsSupported;
  private boolean notificationsEnabled;

  public ServerConnectionModel() {
    this.edit = false;
  }

  public ServerConnectionModel(IServer server) {
    this.edit = true;
    this.serverId = server.getId();
    this.serverUrl = server.getHost();
    this.connectionType = Server.SONARCLOUD_URL.equals(serverUrl) ? ConnectionType.SONARCLOUD : ConnectionType.ONPREMISE;
    this.organization = server.getOrganization();
    if (server.hasAuth()) {
      try {
        this.username = ServersManager.getUsername(server);
        this.password = ServersManager.getPassword(server);
      } catch (StorageException e) {
        SonarLintLogger.get().error(ERROR_READING_SECURE_STORAGE, e);
        MessageDialog.openError(Display.getCurrent().getActiveShell(), ERROR_READING_SECURE_STORAGE, "Unable to read password from secure storage: " + e.getMessage());
      }
      this.authMethod = StringUtils.isBlank(password) ? AuthMethod.TOKEN : AuthMethod.PASSWORD;
    }
    this.notificationsEnabled = server.areNotificationsEnabled();
  }

  public boolean isEdit() {
    return edit;
  }

  public ConnectionType getConnectionType() {
    return connectionType;
  }

  public void setConnectionType(ConnectionType type) {
    ConnectionType old = this.connectionType;
    this.connectionType = type;
    firePropertyChange(PROPERTY_CONNECTION_TYPE, old, this.connectionType);
    if (type == ConnectionType.ONPREMISE) {
      setServerUrl(null);
    } else {
      setServerUrl(Server.SONARCLOUD_URL);
      setAuthMethod(AuthMethod.TOKEN);
    }
  }

  public AuthMethod getAuthMethod() {
    return authMethod;
  }

  public void setAuthMethod(AuthMethod authMethod) {
    AuthMethod old = this.authMethod;
    this.authMethod = authMethod;
    firePropertyChange(PROPERTY_AUTH_METHOD, old, this.authMethod);
    setUsername(null);
    setPassword(null);
  }

  public String getServerId() {
    return serverId;
  }

  public void setServerId(String serverId) {
    String old = this.serverId;
    this.serverId = serverId;
    firePropertyChange(PROPERTY_SERVER_ID, old, this.serverId);
  }

  public String getServerUrl() {
    return serverUrl;
  }

  public void setServerUrl(String serverUrl) {
    String old = this.serverUrl;
    this.serverUrl = serverUrl;
    firePropertyChange(PROPERTY_SERVER_URL, old, this.serverUrl);
    suggestServerId();
  }

  @CheckForNull
  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    String old = this.organization;
    this.organization = organization;
    firePropertyChange(PROPERTY_ORGANIZATION, old, this.organization);
    suggestServerId();
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    String old = this.username;
    this.username = username;
    firePropertyChange(PROPERTY_USERNAME, old, this.username);
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    String old = this.password;
    this.password = password;
    firePropertyChange(PROPERTY_PASSWORD, old, this.password);
  }

  @CheckForNull
  public TextSearchIndex<RemoteOrganization> getOrganizationsIndex() {
    return organizationsIndex;
  }

  public boolean hasOrganizations() {
    return organizationsIndex != null && organizationsIndex.size() > 1;
  }

  public void setOrganizationsIndex(@Nullable TextSearchIndex<RemoteOrganization> organizationsIndex) {
    this.organizationsIndex = organizationsIndex;
  }

  private void suggestServerId() {
    if (!edit) {
      try {
        String suggestedId;
        if (connectionType == ConnectionType.SONARCLOUD) {
          suggestedId = "SonarCloud";
        } else {
          URL url = new URL(getServerUrl());
          suggestedId = url.getHost();
        }
        if (StringUtils.isNotBlank(organization)) {
          suggestedId += "/" + organization;
        }
        setServerId(suggestedId);
      } catch (MalformedURLException e1) {
        // Ignore, should not occurs
      }
    }
  }

  public boolean getNotificationsSupported() {
    return notificationsSupported;
  }

  public void setNotificationsSupported(boolean value) {
    notificationsSupported = value;
  }

  public boolean getNotificationsEnabled() {
    return notificationsEnabled;
  }

  public void setNotificationsEnabled(boolean value) {
    boolean old = this.notificationsEnabled;
    this.notificationsEnabled = value;
    firePropertyChange(PROPERTY_NOTIFICATIONS_ENABLED, old, this.notificationsEnabled);
  }
}
