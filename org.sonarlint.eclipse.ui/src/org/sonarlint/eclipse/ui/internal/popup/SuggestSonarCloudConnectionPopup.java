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
import org.eclipse.swt.widgets.Composite;

public class SuggestSonarCloudConnectionPopup extends AbstractSuggestConnectionPopup {
  public SuggestSonarCloudConnectionPopup(String organization, Map<String, List<String>> sonarToEclipseProjectMapping) {
    super(organization, sonarToEclipseProjectMapping, true);
  }

  @Override
  protected String getMessage() {
    if (sonarToEclipseProjectMapping.keySet().size() > 1) {
      return "For the SonarCloud organization '" + organization + "' there are multiple projects that can be "
        + "connected to local projects. Click 'More Information' to see them all. Do you want to connect and bind "
        + "the project?";
    }

    var projectKey = sonarToEclipseProjectMapping.keySet().toArray()[0];
    var mappedProjects = sonarToEclipseProjectMapping.get(projectKey);
    if (mappedProjects.size() > 1) {
      return "For the SonarCloud organization '" + organization + "' the project '" + projectKey
        + "' can be connected to multiple local projects. Click 'More Information' to see them all. Do you want to "
        + "connect and bind the project?";
    }

    return "For the SonarCloud organization '" + organization + "' the project '" + projectKey
      + "' can be connected to the local project '" + mappedProjects.get(0)
      + "'. Do you want to connect and bind the project?";
  }

  @Override
  protected void createContentArea(Composite composite) {
    super.createContentArea(composite);

    addLinkWithTooltip("Connect", "Connect to organization", e -> {
      close();

      // AssistSuggestConnectionJob and afterwards bind all projects
    });

    var projectKeys = sonarToEclipseProjectMapping.keySet().toArray();
    if (projectKeys.length > 1 || sonarToEclipseProjectMapping.get(projectKeys[0]).size() > 1) {
      addMoreInformationLink();
    }

    addDontAskAgainLink();
  }
}
