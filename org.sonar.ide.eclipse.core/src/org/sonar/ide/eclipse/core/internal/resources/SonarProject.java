/*
 * Sonar Eclipse
 * Copyright (C) 2010-2012 SonarSource
 * dev@sonar.codehaus.org
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
package org.sonar.ide.eclipse.core.internal.resources;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;

import java.util.Date;
import java.util.List;

/**
 * @author Evgeny Mandrikov
 */
public class SonarProject implements ISonarProject {

  private final IProject project;
  private String url;
  private String key;
  private boolean analysedLocally;
  private Date lastAnalysisDate;
  private List<SonarProperty> extraProperties;

  public SonarProject(IProject project) {
    this.project = project;
  }

  public static SonarProject getInstance(IResource resource) {
    if (resource == null) {
      return null;
    }
    IProject project = resource.getProject();
    if ((project == null) || !project.isAccessible()) {
      return null;
    }
    return SonarCorePlugin.getDefault().getProjectManager().readSonarConfiguration(project);
  }

  public void save() {
    SonarCorePlugin.getDefault().getProjectManager().saveSonarConfiguration(project, this);
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public IProject getProject() {
    return project;
  }

  public IResource getResource() {
    return project;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getName() {
    return project.getName();
  }

  public boolean isAnalysedLocally() {
    return analysedLocally;
  }

  public void setAnalysedLocally(boolean value) {
    this.analysedLocally = value;
  }

  public Date getLastAnalysisDate() {
    return lastAnalysisDate == null ? null : new Date(lastAnalysisDate.getTime());
  }

  public void setLastAnalysisDate(Date lastAnalysisDate) {
    this.lastAnalysisDate = lastAnalysisDate == null ? null : new Date(lastAnalysisDate.getTime());
  }

  public List<SonarProperty> getExtraProperties() {
    return extraProperties;
  }

  public void setExtraProperties(List<SonarProperty> extraProperties) {
    this.extraProperties = extraProperties;
  }

}
