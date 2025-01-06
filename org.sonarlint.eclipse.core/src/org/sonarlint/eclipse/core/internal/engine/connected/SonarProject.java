/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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

import java.util.Objects;

public class SonarProject {

  private String connectionId;
  private final String name;
  private final String projectKey;

  public SonarProject(String connectionId, String projectKey, String name) {
    this.connectionId = connectionId;
    this.projectKey = projectKey;
    this.name = name;
  }

  public String getConnectionId() {
    return connectionId;
  }

  public void setUrl(String url) {
    this.connectionId = url;
  }

  public String getProjectKey() {
    return projectKey;
  }

  public String getName() {
    return name;
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectKey, connectionId, name);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if ((obj == null) || (getClass() != obj.getClass())) {
      return false;
    }
    var other = (SonarProject) obj;
    return Objects.equals(projectKey, other.projectKey) && Objects.equals(connectionId, other.connectionId) && Objects.equals(name, other.name);
  }

}
