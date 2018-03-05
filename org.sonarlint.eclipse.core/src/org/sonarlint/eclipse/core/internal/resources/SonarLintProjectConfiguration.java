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
package org.sonarlint.eclipse.core.internal.resources;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.eclipse.core.runtime.preferences.IScopeContext;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;

public class SonarLintProjectConfiguration {

  private final IScopeContext projectScope;
  private List<SonarLintProperty> extraProperties = new ArrayList<>();
  private List<ExclusionItem> fileExclusions = new ArrayList<>();
  private String projectKey;
  private String moduleKey;
  private String serverId;
  private boolean autoEnabled = true;

  SonarLintProjectConfiguration(IScopeContext projectScope) {
    this.projectScope = projectScope;
  }

  public static SonarLintProjectConfiguration read(IScopeContext projectScope) {
    return SonarLintCorePlugin.getInstance().getProjectManager().readSonarLintConfiguration(projectScope);
  }

  public void save() {
    SonarLintCorePlugin.getInstance().getProjectManager().saveSonarLintConfiguration(projectScope, this);
  }

  public List<ExclusionItem> getFileExclusions() {
    return fileExclusions;
  }

  public void setFileExclusions(List<ExclusionItem> fileExclusions) {
    this.fileExclusions = new ArrayList<>(fileExclusions);
  }

  public List<SonarLintProperty> getExtraProperties() {
    return extraProperties;
  }

  public void setExtraProperties(List<SonarLintProperty> extraProperties) {
    this.extraProperties = extraProperties;
  }

  @CheckForNull
  public String getProjectKey() {
    return StringUtils.trimToNull(projectKey);
  }

  @CheckForNull
  public String getModuleKey() {
    return StringUtils.trimToNull(moduleKey);
  }

  @CheckForNull
  public String getServerId() {
    return StringUtils.trimToNull(serverId);
  }

  public void setProjectKey(@Nullable String projectKey) {
    this.projectKey = projectKey;
  }

  public void setModuleKey(@Nullable String moduleKey) {
    this.moduleKey = moduleKey;
  }

  public void setServerId(@Nullable String serverId) {
    this.serverId = serverId;
  }

  public boolean isBound() {
    return getServerId() != null && getModuleKey() != null;
  }

  public boolean isAutoEnabled() {
    return autoEnabled;
  }

  public void setAutoEnabled(boolean autoEnabled) {
    this.autoEnabled = autoEnabled;
  }

  public void unbind() {
    setServerId(null);
    setProjectKey(null);
    setModuleKey(null);
    save();
  }

}
