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
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.graphics.Image;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;

/** Notification pop-up displayed for project key(s) not found on SonarQube / SonarCloud */
public class ProjectKeyNotFoundPopup extends AbstractSonarLintPopup {
  private final List<String> projectKeys;
  @Nullable
  private final String serverUrl;
  @Nullable
  private final String organization;

  public ProjectKeyNotFoundPopup(List<String> projectKeys, @Nullable String serverUrl,
    @Nullable String organization) {
    this.projectKeys = projectKeys.stream().map(projectKey -> "'" + projectKey + "'").collect(Collectors.toList());
    this.serverUrl = serverUrl;
    this.organization = organization;
  }

  @Override
  protected String getMessage() {
    var suffix = serverUrl != null
      ? (" on server '" + serverUrl + "'.")
      : (" in organization '" + organization + "'.");
    var message = projectKeys.size() == 1
      ? ("The project key '" + projectKeys.get(0) + "' was not found")
      : ("The project keys " + String.join(",", projectKeys) + " were not found");
    return message + suffix;
  }

  @Override
  protected String getPopupShellTitle() {
    return "Project key(s) not found on " + (serverUrl != null ? "SonarQube" : "SonarCloud");
  }

  @Override
  protected Image getPopupShellImage(int maximumHeight) {
    return SonarLintImages.IMG_SEVERITY_BLOCKER;
  }
}
