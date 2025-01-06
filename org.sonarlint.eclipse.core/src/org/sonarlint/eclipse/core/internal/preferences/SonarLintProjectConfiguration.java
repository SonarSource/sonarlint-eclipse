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
package org.sonarlint.eclipse.core.internal.preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.internal.resources.ExclusionItem;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProperty;

public class SonarLintProjectConfiguration {

  private final List<SonarLintProperty> extraProperties = new ArrayList<>();
  private final List<ExclusionItem> fileExclusions = new ArrayList<>();
  @Nullable
  private EclipseProjectBinding projectBinding;
  private boolean autoEnabled = true;
  private boolean bindingSuggestionsDisabled = false;
  private boolean indexingBasedOnEclipsePlugIns = true;

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

  public static class EclipseProjectBinding {

    private final String connectionId;
    private final String projectKey;

    public EclipseProjectBinding(String connectionId, String projectKey) {
      this.connectionId = connectionId;
      this.projectKey = projectKey;
    }

    public String getConnectionId() {
      return connectionId;
    }

    public String getProjectKey() {
      return projectKey;
    }

    @Override
    public int hashCode() {
      return Objects.hash(connectionId, projectKey);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!super.equals(obj) || (getClass() != obj.getClass())) {
        return false;
      }
      var other = (EclipseProjectBinding) obj;
      return Objects.equals(connectionId, other.connectionId) && Objects.equals(projectKey, other.projectKey);
    }

  }

  public boolean isBindingSuggestionsDisabled() {
    return this.bindingSuggestionsDisabled;
  }

  public void setBindingSuggestionsDisabled(boolean bindingSuggestionsDisabled) {
    this.bindingSuggestionsDisabled = bindingSuggestionsDisabled;
  }

  public boolean isIndexingBasedOnEclipsePlugIns() {
    return this.indexingBasedOnEclipsePlugIns;
  }

  public void setIndexingBasedOnEclipsePlugIns(boolean indexingBasedOnEclipsePlugIns) {
    this.indexingBasedOnEclipsePlugIns = indexingBasedOnEclipsePlugIns;
  }
}
