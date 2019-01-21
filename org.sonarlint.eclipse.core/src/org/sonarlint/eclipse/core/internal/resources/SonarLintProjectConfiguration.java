/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2019 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonarsource.sonarlint.core.client.api.connected.ProjectBinding;

public class SonarLintProjectConfiguration {

  private List<SonarLintProperty> extraProperties = new ArrayList<>();
  private List<ExclusionItem> fileExclusions = new ArrayList<>();
  @Nullable
  private EclipseProjectBinding projectBinding;
  private boolean autoEnabled = true;

  public List<ExclusionItem> getFileExclusions() {
    return fileExclusions;
  }

  public List<SonarLintProperty> getExtraProperties() {
    return extraProperties;
  }

  public boolean isBound() {
    return projectBinding != null;
  }

  public boolean isAutoEnabled() {
    return autoEnabled;
  }

  public void setAutoEnabled(boolean autoEnabled) {
    this.autoEnabled = autoEnabled;
  }

  public void setProjectBinding(@Nullable EclipseProjectBinding projectBinding) {
    this.projectBinding = projectBinding;
  }

  public Optional<EclipseProjectBinding> getProjectBinding() {
    return Optional.ofNullable(projectBinding);
  }

  public static class EclipseProjectBinding extends ProjectBinding {

    private final String serverId;

    public EclipseProjectBinding(String serverId, String projectKey, String sqPathPrefix, String idePathPrefix) {
      super(projectKey, sqPathPrefix, idePathPrefix);
      this.serverId = serverId;
    }

    public String serverId() {
      return serverId;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((serverId == null) ? 0 : serverId.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!super.equals(obj)) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      EclipseProjectBinding other = (EclipseProjectBinding) obj;
      return Objects.equals(serverId, other.serverId);
    }

  }

}
