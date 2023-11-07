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
package org.sonarlint.eclipse.ui.internal.binding.wizard.project;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectionFacade;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.util.wizard.ModelObject;

public class ProjectBindingModel extends ModelObject {

  public static final String PROPERTY_CONNECTION = "connection";
  public static final String PROPERTY_SONAR_PROJECT_KEY = "sonarProjectKey";
  public static final String PROPERTY_PROJECTS = "eclipseProjects";

  private List<ISonarLintProject> eclipseProjects;
  private ConnectionFacade connection;
  private String sonarProjectKey;
  private boolean skipConnectionSelection;

  public void setProjects(List<ISonarLintProject> eclipseProjects) {
    this.eclipseProjects = eclipseProjects;
  }

  @Nullable
  public ConnectionFacade getConnection() {
    return connection;
  }

  public void setConnection(ConnectionFacade connection) {
    var old = this.connection;
    this.connection = connection;
    firePropertyChange(PROPERTY_CONNECTION, old, this.connection);
  }

  public String getSonarProjectKey() {
    return sonarProjectKey;
  }

  public void setSonarProjectKey(String sonarProjectKey) {
    var old = this.sonarProjectKey;
    this.sonarProjectKey = sonarProjectKey;
    firePropertyChange(PROPERTY_SONAR_PROJECT_KEY, old, this.sonarProjectKey);
  }

  public List<ISonarLintProject> getEclipseProjects() {
    return eclipseProjects;
  }

  public void setEclipseProjects(List<ISonarLintProject> eclipseProjects) {
    var old = this.eclipseProjects;
    this.eclipseProjects = new ArrayList<>(eclipseProjects);
    firePropertyChange(PROPERTY_CONNECTION, old, this.eclipseProjects);
  }

  public boolean isSkipConnectionSelection() {
    return skipConnectionSelection;
  }

  public void setSkipServer(boolean skipConnectionSelection) {
    this.skipConnectionSelection = skipConnectionSelection;
  }

}
