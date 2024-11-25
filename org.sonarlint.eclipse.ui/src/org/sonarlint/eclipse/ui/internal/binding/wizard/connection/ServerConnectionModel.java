/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.binding.wizard.connection;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectionFacade;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectionManager;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.util.wizard.ModelObject;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.connection.org.OrganizationDto;
import org.sonarsource.sonarlint.core.rpc.protocol.common.Either;
import org.sonarsource.sonarlint.core.rpc.protocol.common.TokenDto;
import org.sonarsource.sonarlint.core.rpc.protocol.common.UsernamePasswordDto;

public class ServerConnectionModel extends ModelObject {

  private static final String ERROR_READING_SECURE_STORAGE = "Error reading secure storage";

  public static final String PROPERTY_CONNECTION_TYPE = "connectionType";
  public static final String PROPERTY_SERVER_URL = "serverUrl";
  public static final String PROPERTY_AUTH_METHOD = "authMethod";
  public static final String PROPERTY_USERNAME = "username";
  public static final String PROPERTY_PASSWORD = "password";
  public static final String PROPERTY_ORGANIZATION = "organization";
  public static final String PROPERTY_CONNECTION_ID = "connectionId";
  public static final String PROPERTY_NOTIFICATIONS_ENABLED = "notificationsEnabled";

  public enum ConnectionType {
    SONARCLOUD, ONPREMISE
  }

  private final boolean edit;
  private final boolean fromConnectionSuggestion;
  private ConnectionType connectionType = ConnectionType.SONARCLOUD;
  private String connectionId;
  private String serverUrl = SonarLintUtils.getSonarCloudUrl();
  private String organization;
  private String username;

  /**
   *  @deprecated as only token authentication is supported from now on and this is saved in the username field!
   */
  @Deprecated(since = "10.10", forRemoval = true)
  @Nullable
  private String password;

  private boolean notificationsSupported;
  private boolean notificationsDisabled;

  private List<ISonarLintProject> selectedProjects;

  public ServerConnectionModel(boolean fromConnectionSuggestion) {
    this.edit = false;
    this.fromConnectionSuggestion = fromConnectionSuggestion;
  }

  public ServerConnectionModel() {
    this(false);
  }

  public ServerConnectionModel(ConnectionFacade connection) {
    this.edit = true;
    this.fromConnectionSuggestion = false;
    this.connectionId = connection.getId();
    this.serverUrl = connection.getHost();
    this.connectionType = SonarLintUtils.getSonarCloudUrl().equals(serverUrl) ? ConnectionType.SONARCLOUD : ConnectionType.ONPREMISE;
    this.organization = connection.getOrganization();
    if (connection.hasAuth()) {
      try {
        this.username = ConnectionManager.getUsername(connection);
        this.password = ConnectionManager.getPassword(connection);
      } catch (StorageException e) {
        SonarLintLogger.get().error(ERROR_READING_SECURE_STORAGE, e);
        MessageDialog.openError(Display.getCurrent().getActiveShell(), ERROR_READING_SECURE_STORAGE, "Unable to read password from secure storage: " + e.getMessage());
      }
    }
    this.notificationsDisabled = connection.areNotificationsDisabled();
  }

  public boolean isEdit() {
    return edit;
  }

  public boolean isFromConnectionSuggestion() {
    return fromConnectionSuggestion;
  }

  public ConnectionType getConnectionType() {
    return connectionType;
  }

  public void setConnectionType(ConnectionType type) {
    var old = this.connectionType;
    this.connectionType = type;
    firePropertyChange(PROPERTY_CONNECTION_TYPE, old, this.connectionType);
    if (type == ConnectionType.ONPREMISE) {
      setServerUrl(null);
    } else {
      setServerUrl(SonarLintUtils.getSonarCloudUrl());
    }
  }

  public String getConnectionId() {
    return connectionId;
  }

  public void setConnectionId(String connectionId) {
    var old = this.connectionId;
    this.connectionId = connectionId;
    firePropertyChange(PROPERTY_CONNECTION_ID, old, this.connectionId);
  }

  public String getServerUrl() {
    return serverUrl;
  }

  public void setServerUrl(String serverUrl) {
    var old = this.serverUrl;
    this.serverUrl = serverUrl;
    firePropertyChange(PROPERTY_SERVER_URL, old, this.serverUrl);
    suggestServerId();
  }

  @Nullable
  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    var old = this.organization;
    this.organization = organization;
    firePropertyChange(PROPERTY_ORGANIZATION, old, this.organization);
    suggestServerId();
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    var old = this.username;
    this.username = username;
    firePropertyChange(PROPERTY_USERNAME, old, this.username);
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    var old = this.password;
    this.password = password;
    firePropertyChange(PROPERTY_PASSWORD, old, this.password);
  }

  public void suggestOrganization(List<OrganizationDto> userOrgs) {
    if (!isEdit() && userOrgs != null && userOrgs.size() == 1) {
      setOrganization(userOrgs.get(0).getKey());
    }
  }

  private void suggestServerId() {
    if (!edit) {
      try {
        String suggestedId;
        if (connectionType == ConnectionType.SONARCLOUD) {
          suggestedId = "SonarCloud";
        } else {
          var url = new URL(getServerUrl());
          suggestedId = url.getHost();
        }
        if (StringUtils.isNotBlank(organization)) {
          suggestedId += "/" + organization;
        }
        setConnectionId(suggestedId);
      } catch (MalformedURLException e1) {
        // Ignore, should not occurs
      }
    }
  }

  public boolean getNotificationsSupported() {
    return notificationsSupported;
  }

  /**
   * Used by bean binding
   */
  public void setNotificationsSupported(boolean value) {
    notificationsSupported = value;
  }

  /**
   * Used by bean binding
   */
  public boolean getNotificationsEnabled() {
    return !notificationsDisabled;
  }

  public void setNotificationsEnabled(boolean value) {
    var old = !this.notificationsDisabled;
    this.notificationsDisabled = !value;
    firePropertyChange(PROPERTY_NOTIFICATIONS_ENABLED, old, !this.notificationsDisabled);
  }

  public void setSelectedProjects(List<ISonarLintProject> selectedProjects) {
    this.selectedProjects = selectedProjects;
  }

  @Nullable
  public List<ISonarLintProject> getSelectedProjects() {
    return selectedProjects;
  }

  public boolean getNotificationsDisabled() {
    return this.notificationsDisabled;
  }

  public Either<TokenDto, UsernamePasswordDto> getTransientRpcCrendentials() {
    if (password == null) {
      return Either.forLeft(new TokenDto(username));
    } else {
      return Either.forRight(new UsernamePasswordDto(username, password));
    }
  }
}
