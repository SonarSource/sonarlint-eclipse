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
package org.sonarlint.eclipse.core.internal.server;

import java.util.Objects;

/**
 * This is one possible result that will be displayed as a suggestion.
 * Because content assist can only work with String and not with complex objects
 * we will have to "serialize" this object using {@link #asString()} in
 * {@link SearchEngineProvider#getProposals(String, int)}
 * then deserialize in the {@link RemoteProjectTextContentAdapter}
 *
 */
public class RemoteSonarProject {

  private static final String SEPARATOR = "|";

  private String serverId;
  private String name;
  private String projectKey;

  public RemoteSonarProject(String serverId, String projectKey, String name) {
    this.serverId = serverId;
    this.projectKey = projectKey;
    this.name = name;
  }

  public String getServerId() {
    return serverId;
  }

  public void setUrl(String url) {
    this.serverId = url;
  }

  public String getProjectKey() {
    return projectKey;
  }

  public void setProjectKey(String projectKey) {
    this.projectKey = projectKey;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String asString() {
    return serverId + SEPARATOR + projectKey + SEPARATOR + name;
  }

  /**
   * The description that will be displayed in Content assist.
   */
  public String getDescription() {
    StringBuilder sb = new StringBuilder();
    sb.append("Project name: ").append(name).append("\n");
    sb.append("Server: ").append(serverId).append("\n");
    sb.append("Project key: ").append(projectKey);
    return sb.toString();
  }

  public static RemoteSonarProject fromString(String asString) {
    String[] parts = asString.split("\\" + SEPARATOR);
    return new RemoteSonarProject(parts[0], parts[1], parts[2]);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectKey, serverId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    RemoteSonarProject other = (RemoteSonarProject) obj;
    return Objects.equals(projectKey, other.projectKey) && Objects.equals(serverId, other.serverId);
  }

}
