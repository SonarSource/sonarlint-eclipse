/*
 * SonarLint for Eclipse
 * Copyright (C) 2015 SonarSource
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
package org.sonarlint.eclipse.core.internal.resources;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;

public class SonarLintProject {

  private final IProject project;
  private List<SonarLintProperty> extraProperties = new ArrayList<SonarLintProperty>();

  public SonarLintProject(IProject project) {
    this.project = project;
  }

  public static SonarLintProject getInstance(IResource resource) {
    IProject project = resource.getProject();
    if (project == null || !project.isAccessible()) {
      throw new IllegalStateException("Unable to find project for resource " + resource);
    }
    return SonarLintCorePlugin.getDefault().getProjectManager().readSonarConfiguration(project);
  }

  public void save() {
    SonarLintCorePlugin.getDefault().getProjectManager().saveSonarConfiguration(project, this);
  }

  public IProject getProject() {
    return project;
  }

  public List<SonarLintProperty> getExtraProperties() {
    return extraProperties;
  }

  public void setExtraProperties(List<SonarLintProperty> extraProperties) {
    this.extraProperties = extraProperties;
  }

  public String getServerUrl() {
    return SonarLintCorePlugin.getDefault().getRunner().getUrl();
  }

}
