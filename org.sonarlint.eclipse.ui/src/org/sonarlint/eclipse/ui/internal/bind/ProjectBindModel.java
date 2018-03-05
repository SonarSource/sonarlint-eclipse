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
package org.sonarlint.eclipse.ui.internal.bind;

import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

/**
 * This class represents the association between an Eclipse and a SonarQube project/module.
 *
 */
public class ProjectBindModel extends AbstractModelObject {
  public static final String PROPERTY_PROJECT_ECLIPSE_NAME = "eclipseName";
  public static final String PROPERTY_PROJECT_SONAR_FULLNAME = "displayName";

  private final ISonarLintProject project;
  private String projectKey;
  private String moduleKey;
  private String serverId;
  private IServer server;
  private boolean autoBindFailed;

  public ProjectBindModel(ISonarLintProject project) {
    this.project = project;
    SonarLintProjectConfiguration projectConfig = SonarLintProjectConfiguration.read(project.getScopeContext());
    this.projectKey = projectConfig.getProjectKey();
    this.moduleKey = projectConfig.getModuleKey();
    this.serverId = projectConfig.getServerId();
    this.server = SonarLintCorePlugin.getServersManager().getServer(this.serverId);
  }

  public ISonarLintProject getProject() {
    return project;
  }

  public String getEclipseName() {
    return project.getName();
  }

  public String getDisplayName() {
    if (StringUtils.isBlank(moduleKey)) {
      if (autoBindFailed) {
        return "<Auto-bind failed. Type here to start searching for a remote SonarQube project...>";
      }
      return "< Type here to start searching for a remote project or enter the exact project key...>";
    } else if (server == null) {
      return "<Bound to an unknown server: '" + this.serverId + "'>";
    } else {
      return String.format("'%s' on server '%s'", moduleKey, server.getId());
    }
  }

  public String getProjectKey() {
    return projectKey;
  }

  public String getModuleKey() {
    return moduleKey;
  }

  public void associate(String serverId, String projectKey, String moduleKey) {
    String oldValue = getDisplayName();
    this.autoBindFailed = false;
    this.projectKey = projectKey;
    this.moduleKey = moduleKey;
    this.serverId = serverId;
    this.server = SonarLintCorePlugin.getServersManager().getServer(this.serverId);
    firePropertyChange(PROPERTY_PROJECT_SONAR_FULLNAME, oldValue, getDisplayName());
  }

  public void unassociate() {
    String oldValue = getDisplayName();
    this.autoBindFailed = false;
    resetBinding();
    firePropertyChange(PROPERTY_PROJECT_SONAR_FULLNAME, oldValue, getDisplayName());
  }

  public String getServerId() {
    return serverId;
  }

  public void setAutoBindFailed(boolean autoBindFailed) {
    String oldValue = getDisplayName();
    this.autoBindFailed = autoBindFailed;
    resetBinding();
    firePropertyChange(PROPERTY_PROJECT_SONAR_FULLNAME, oldValue, getDisplayName());
  }

  private void resetBinding() {
    this.projectKey = null;
    this.moduleKey = null;
    this.serverId = null;
    this.server = null;
  }

}
