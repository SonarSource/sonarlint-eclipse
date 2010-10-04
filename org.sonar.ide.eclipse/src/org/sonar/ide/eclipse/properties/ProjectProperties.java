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
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.preferences.PreferenceConstants;

/**
 * @author Evgeny Mandrikov
 */
public class ProjectProperties {

  private IProject project;
  private String url;
  private String groupId;
  private String artifactId;
  private String branch;

  public ProjectProperties(IProject project) {
    this.project = project;
  }

  public static ProjectProperties getInstance(IResource resource) {
    if (resource == null) {
      return null;
    }
    IProject project = resource.getProject();
    if (project == null || !project.isAccessible()) {
      return null;
    }
    return SonarPlugin.getDefault().getProjectManager().readSonarConfiguration(project);
  }

  public void save() {
    SonarPlugin.getDefault().getProjectManager().saveSonarConfiguration(project, this);
  }

  public String getUrl() {
    if (StringUtils.isBlank(url)) {
      url = SonarPlugin.getDefault().getPreferenceStore().getString(PreferenceConstants.P_SONAR_SERVER_URL);
    }
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }

  public String getBranch() {
    return branch;
  }

  public void setBranch(String branch) {
    this.branch = branch;
  }

  /**
   * @deprecated since 0.3 use {@link SonarPlugin#hasSonarNature(IProject)}
   */
  @Deprecated
  public boolean isProjectConfigured() {
    return StringUtils.isNotBlank(getArtifactId()) && StringUtils.isNotBlank(getGroupId()) && StringUtils.isNotBlank(getUrl());
  }
}
