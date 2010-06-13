/*
 * Copyright (C) 2010 Evgeny Mandrikov
 *
 * Sonar-IDE is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar-IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar-IDE; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.properties;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.sonar.ide.api.SonarIdeException;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.preferences.PreferenceConstants;
import org.sonar.ide.shared.AbstractProjectProperties;

/**
 * TODO Godin: Don't extend AbstractProjectProperties and move into commons library
 * 
 * @author Evgeny Mandrikov
 */
public class ProjectProperties extends AbstractProjectProperties<IProject> {

  private String url;
  private String groupId;
  private String artifactId;
  private String branch;

  public ProjectProperties(IProject project) {
    super(project);
  }

  public static ProjectProperties getInstance(IResource resource) {
    if (resource == null) {
      return null;
    }
    IProject project = resource.getProject();
    if (project == null || !project.isAccessible()) {
      return null;
    }
    // FIXME Godin: most probably that method "find" causes SONARIDE-121
//    ProjectProperties projectProperties = (ProjectProperties) find(project.getName());
//    if (projectProperties != null) {
//      return projectProperties;
//    }
    return SonarPlugin.getDefault().getProjectManager().readSonarConfiguration(project);
  }

  @Override
  public void load() {
  }

  @Override
  public void save() {
    SonarPlugin.getDefault().getProjectManager().saveSonarConfiguration(getProject(), this);
  }

  @Override
  public String getUrl() {
    if (StringUtils.isBlank(url)) {
      url = SonarPlugin.getDefault().getPreferenceStore().getString(PreferenceConstants.P_SONAR_SERVER_URL);
    }
    return url;
  }

  @Override
  public void setUrl(String url) {
    this.url = url;
  }

  @Override
  public String getGroupId() {
    return groupId;
  }

  @Override
  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  @Override
  public String getArtifactId() {
    if (StringUtils.isBlank(artifactId)) {
      artifactId = getProjectName();
    }
    return artifactId;
  }

  @Override
  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }
  
  @Override
  public String getBranch() {
    return branch;
  }
  
  @Override
  public void setBranch(String branch) {
    this.branch = branch;
  }

  @Override
  protected String getProjectName() {
    return getProject().getName();
  }

  @Override
  protected String getProperty(String type, String defaultValue) {
    throw new SonarIdeException("Don't use this method");
  }

  @Override
  protected void setProperty(String type, String value) {
    throw new SonarIdeException("Don't use this method");
  }

  public boolean isProjectConfigured() {
    return StringUtils.isNotBlank(getArtifactId()) && StringUtils.isNotBlank(getGroupId()) && StringUtils.isNotBlank(getUrl());
  }
}
