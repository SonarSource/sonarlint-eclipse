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
package org.sonar.ide.eclipse.core.internal.resources;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.servers.SonarServer;
import org.sonar.ide.eclipse.core.resources.ISonarProject;

public class SonarProject implements ISonarProject {

  private final IProject project;
  private String serverId;
  private String key;
  private List<SonarProperty> extraProperties = new ArrayList<SonarProperty>();

  public SonarProject(IProject project) {
    this.project = project;
  }

  public static SonarProject getInstance(IResource resource) {
    IProject project = resource.getProject();
    if (project == null || !project.isAccessible()) {
      throw new IllegalStateException("Unable to find project for resource " + resource);
    }
    return SonarCorePlugin.getDefault().getProjectManager().readSonarConfiguration(project);
  }

  public void save() {
    SonarCorePlugin.getDefault().getProjectManager().saveSonarConfiguration(project, this);
  }

  @CheckForNull
  public SonarServer getServer() {
    return SonarCorePlugin.getServersManager().findServer(serverId);
  }

  public void setServerId(String id) {
    this.serverId = id;
  }

  @Override
  public IProject getProject() {
    return project;
  }

  @Override
  public IResource getResource() {
    return project;
  }

  @Override
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  @Override
  public String getName() {
    return project.getName();
  }

  public List<SonarProperty> getExtraProperties() {
    return extraProperties;
  }

  public void setExtraProperties(List<SonarProperty> extraProperties) {
    this.extraProperties = extraProperties;
  }

  public String getServerId() {
    return serverId;
  }

  @Override
  public boolean isAssociated() {
    return StringUtils.isNotBlank(serverId);
  }

}
