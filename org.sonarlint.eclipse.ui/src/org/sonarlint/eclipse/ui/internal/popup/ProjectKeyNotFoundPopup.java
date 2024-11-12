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
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarsource.sonarlint.core.rpc.protocol.common.Either;

/** Notification pop-up displayed for project key(s) not found on SonarQube / SonarCloud */
public class ProjectKeyNotFoundPopup extends AbstractSonarLintPopup {
  private final List<String> projectKeys;
  private final Either<String, String> serverUrlOrOrganization;

  public ProjectKeyNotFoundPopup(List<String> projectKeys, Either<String, String> serverUrlOrOrganization) {
    this.projectKeys = projectKeys.stream().map(projectKey -> "'" + projectKey + "'").collect(Collectors.toList());
    this.serverUrlOrOrganization = serverUrlOrOrganization;
  }

  @Override
  protected String getMessage() {
    String suffix;
    if (serverUrlOrOrganization.isLeft()) {
      suffix = " on server '" + serverUrlOrOrganization.getLeft() + "'.";
    } else {
      suffix = " in organization '" + serverUrlOrOrganization.getRight() + "'.";
    }

    var message = projectKeys.size() == 1
      ? ("The project key '" + projectKeys.get(0) + "' was not found")
      : ("The project keys " + String.join(",", projectKeys) + " were not found");

    return message + suffix;
  }

  @Override
  protected String getPopupShellTitle() {
    return "Project key(s) not found on SonarQube "
      + (serverUrlOrOrganization.isLeft() ? "Server" : "Cloud");
  }

  @Override
  protected Image getPopupShellImage(int maximumHeight) {
    return SonarLintImages.IMG_SEVERITY_BLOCKER;
  }
}
