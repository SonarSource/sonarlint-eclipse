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
package org.sonarlint.eclipse.ui.internal.popup;

import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.swt.graphics.Image;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;

/** Notification pop-up displayed for project(s) bound to SonarQube / SonarCloud */
public class ProjectBoundPopup extends AbstractSonarLintToastPopup {
  private final String projectKey;
  private final List<String> projects;
  private final boolean isSonarCloud;

  public ProjectBoundPopup(String projectKey, List<ISonarLintProject> projects, boolean isSonarCloud) {
    super(10000);
    this.projectKey = projectKey;
    this.projects = projects.stream().map(project -> "'" + project.getName() + "'").collect(Collectors.toList());
    this.isSonarCloud = isSonarCloud;
  }

  @Override
  protected String getMessage() {
    var message = projects.size() == 1
      ? ("The project '" + projects.get(0) + "' is bound to '")
      : ("The projects " + String.join(",", projects) + " are bound to '");
    return message + projectKey + "'.";
  }

  @Override
  protected String getPopupShellTitle() {
    return "Project(s) bound to SonarQube " + (isSonarCloud ? "Cloud" : "Server");
  }

  @Override
  protected Image getPopupShellImage(int maximumHeight) {
    return isSonarCloud
      ? SonarLintImages.SONARCLOUD_SERVER_ICON_IMG
      : SonarLintImages.SONARQUBE_SERVER_ICON_IMG;
  }
}
