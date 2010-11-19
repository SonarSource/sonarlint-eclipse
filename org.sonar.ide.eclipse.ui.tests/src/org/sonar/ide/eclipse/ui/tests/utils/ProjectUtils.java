/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.ui.tests.utils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.sonar.ide.eclipse.internal.ui.actions.ToggleNatureAction;
import org.sonar.ide.eclipse.internal.ui.properties.ProjectProperties;

public class ProjectUtils {

  public static void configureProject(String name) throws Exception {
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
    configureProject(project, getSonarServerUrl(), "org.sonar-ide.tests", name);
  }

  // TODO should be in core
  public static void configureProject(IProject project, String url, String groupId, String artifactId) throws Exception {
    ProjectProperties properties = ProjectProperties.getInstance(project);
    properties.setUrl(url);
    properties.setGroupId(groupId);
    properties.setArtifactId(artifactId);
    properties.save();
    ToggleNatureAction.enableNature(project);
  }

  public static String getSonarServerUrl() {
    return "http://localhost:9000";
  }

}
