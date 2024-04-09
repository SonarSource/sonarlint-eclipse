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
import java.util.Map;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.graphics.Image;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;

public abstract class AbstractSuggestConnectionPopup extends AbstractSonarLintPopup {
  @Nullable
  protected final String serverUrl;
  @Nullable
  protected final String organization;
  // TODO: Find a better name!
  protected final Map<String, List<String>> sonarToEclipseProjectMapping;

  protected AbstractSuggestConnectionPopup(String serverUrlOrOrganization,
    Map<String, List<String>> sonarToEclipseProjectMapping, boolean isSonarCloud) {
    serverUrl = isSonarCloud ? null : serverUrlOrOrganization;
    organization = isSonarCloud ? serverUrlOrOrganization : null;
    this.sonarToEclipseProjectMapping = sonarToEclipseProjectMapping;
  }

  protected void addDontAskAgainLink() {
    addLink("Don't ask again", e -> {
      close();
      // TODO: Some global configuration based on the serverUrl / organization!
    });
  }

  protected void addMoreInformationLink() {
    addLink("More information", e -> {
      // TODO: Dialog should display all the mappings if there is more than one projectKey!
      // TODO: If there is only one projectKey, this link is not shown at all!
    });
  }

  @Override
  protected String getPopupShellTitle() {
    return "SonarLint Connection Suggestion to " + (serverUrl != null ? "SonarQube" : "SonarCloud");
  }

  @Override
  protected Image getPopupShellImage(int maximumHeight) {
    return serverUrl != null
      ? SonarLintImages.SONARQUBE_SERVER_ICON_IMG
      : SonarLintImages.SONARCLOUD_SERVER_ICON_IMG;
  }
}
