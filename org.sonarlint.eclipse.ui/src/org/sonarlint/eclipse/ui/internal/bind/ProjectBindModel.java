/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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

import org.eclipse.core.resources.IProject;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProject;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;

/**
 * This class represents the association between an Eclipse and a SonarQube project/module.
 *
 */
public class ProjectBindModel extends AbstractModelObject {
  public static final String PROPERTY_PROJECT_ECLIPSE_NAME = "eclipseName";
  public static final String PROPERTY_PROJECT_SONAR_FULLNAME = "sonarFullName";

  private final IProject project;
  private String sonarProjectName;
  private String key;
  private String serverId;

  public ProjectBindModel(IProject project) {
    this.project = project;
    SonarLintProject sonarProject = SonarLintProject.getInstance(project);
    this.key = sonarProject.getModuleKey();
    this.serverId = sonarProject.getServerId();
    this.sonarProjectName = "<Searching name...>";
  }

  public IProject getProject() {
    return project;
  }

  public String getEclipseName() {
    return project.getName();
  }

  public String getSonarFullName() {
    if (StringUtils.isBlank(key)) {
      return "<Type here to start searching for a remote SonarQube project...>";
    } else {
      return sonarProjectName + " on " + serverId + " (" + key + ")";
    }
  }

  public String getKey() {
    return key;
  }

  public void associate(String serverId, String sonarProjectName, String key) {
    String oldValue = getSonarFullName();
    this.key = key;
    this.serverId = serverId;
    this.sonarProjectName = sonarProjectName;
    firePropertyChange(PROPERTY_PROJECT_SONAR_FULLNAME, oldValue, getSonarFullName());
  }

  public void unassociate() {
    String oldValue = getSonarFullName();
    this.key = null;
    this.serverId = null;
    this.sonarProjectName = null;
    firePropertyChange(PROPERTY_PROJECT_SONAR_FULLNAME, oldValue, getSonarFullName());
  }

  public String getServerId() {
    return serverId;
  }

  public String getSonarProjectName() {
    return sonarProjectName;
  }
}
