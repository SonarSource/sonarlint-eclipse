/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.ui.internal.wizards.associate;

import org.eclipse.core.resources.IProject;
import org.sonar.ide.eclipse.core.internal.SonarNature;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.ui.internal.AbstractModelObject;

/**
 * This class represents the association between an Eclipse and a Sonar project.
 * @author julien
 *
 */
public class ProjectAssociationModel extends AbstractModelObject {
  public static final String PROPERTY_PROJECT_ECLIPSE_NAME = "eclipseName";
  public static final String PROPERTY_PROJECT_SONAR_FULLNAME = "sonarFullName";

  private final IProject project;
  private String sonarProjectName;
  private String key;
  private String url;

  public ProjectAssociationModel(IProject project) {
    this.project = project;
    if (SonarNature.hasSonarNature(project)) {
      SonarProject sonarProject = SonarProject.getInstance(project);
      this.key = sonarProject.getKey();
      this.url = sonarProject.getUrl();
      this.sonarProjectName = "<Searching name...>";
    }
  }

  public IProject getProject() {
    return project;
  }

  public String getEclipseName() {
    return project.getName();
  }

  public String getSonarFullName() {
    if (key == null) {
      return "<Click here to start searching for a remote SonarQube project...>";
    } else {
      return sonarProjectName + " on " + url + " (" + key + ")";
    }
  }

  public String getKey() {
    return key;
  }

  public void associate(String url, String sonarProjectName, String key) {
    String oldValue = getSonarFullName();
    this.key = key;
    this.url = url;
    this.sonarProjectName = sonarProjectName;
    firePropertyChange(PROPERTY_PROJECT_SONAR_FULLNAME, oldValue, getSonarFullName());
  }

  public void unassociate() {
    String oldValue = getSonarFullName();
    this.key = null;
    this.url = null;
    this.sonarProjectName = null;
    firePropertyChange(PROPERTY_PROJECT_SONAR_FULLNAME, oldValue, getSonarFullName());
  }

  public String getUrl() {
    return url;
  }

  public String getSonarProjectName() {
    return sonarProjectName;
  }
}
