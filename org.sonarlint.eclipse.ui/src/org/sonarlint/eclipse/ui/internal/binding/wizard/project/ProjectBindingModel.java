/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2022 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.binding.wizard.project;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectedEngineFacade;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.util.wizard.ModelObject;
import org.sonarsource.sonarlint.core.client.api.util.TextSearchIndex;
import org.sonarsource.sonarlint.core.serverapi.component.ServerProject;

public class ProjectBindingModel extends ModelObject {

  public static final String PROPERTY_SERVER = "server";
  public static final String PROPERTY_REMOTE_PROJECT_KEY = "remoteProjectKey";
  public static final String PROPERTY_PROJECTS = "eclipseProjects";

  private List<ISonarLintProject> eclipseProjects;
  private ConnectedEngineFacade server;
  private String remoteProjectKey;
  private boolean skipServerSelection;
  private TextSearchIndex<ServerProject> projectIndex;

  public void setProjects(List<ISonarLintProject> eclipseProjects) {
    this.eclipseProjects = eclipseProjects;
  }

  @Nullable
  public ConnectedEngineFacade getServer() {
    return server;
  }

  public void setServer(ConnectedEngineFacade server) {
    var old = this.server;
    this.server = server;
    firePropertyChange(PROPERTY_SERVER, old, this.server);
  }

  public String getRemoteProjectKey() {
    return remoteProjectKey;
  }

  public void setRemoteProjectKey(String remoteProjectKey) {
    var old = this.remoteProjectKey;
    this.remoteProjectKey = remoteProjectKey;
    firePropertyChange(PROPERTY_REMOTE_PROJECT_KEY, old, this.remoteProjectKey);
  }

  public List<ISonarLintProject> getEclipseProjects() {
    return eclipseProjects;
  }

  public void setEclipseProjects(List<ISonarLintProject> eclipseProjects) {
    var old = this.eclipseProjects;
    this.eclipseProjects = new ArrayList<>(eclipseProjects);
    firePropertyChange(PROPERTY_SERVER, old, this.eclipseProjects);
  }

  public boolean isSkipServerSelection() {
    return skipServerSelection;
  }

  public void setSkipServer(boolean skipServerSelection) {
    this.skipServerSelection = skipServerSelection;
  }

  @Nullable
  public TextSearchIndex<ServerProject> getProjectIndex() {
    return projectIndex;
  }

  public void setProjectIndex(TextSearchIndex<ServerProject> projectIndex) {
    this.projectIndex = projectIndex;

  }

}
