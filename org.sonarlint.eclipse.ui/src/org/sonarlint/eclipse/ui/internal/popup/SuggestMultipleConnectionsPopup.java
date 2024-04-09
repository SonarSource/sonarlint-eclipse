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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarsource.sonarlint.core.rpc.protocol.client.connection.ConnectionSuggestionDto;

public class SuggestMultipleConnectionsPopup extends AbstractSonarLintPopup {
  protected final String configScopeId;
  protected final List<ConnectionSuggestionDto> suggestions;

  public SuggestMultipleConnectionsPopup(String configScopeId, List<ConnectionSuggestionDto> suggestions) {
    this.configScopeId = configScopeId;
    this.suggestions = suggestions;
  }

  protected void addDontAskAgainLink() {
    addLink("Don't ask again", e -> {
      close();
      // TODO: Some global configuration based on configScopeId
    });
  }

  protected void addMoreInformationLink() {
    addLink("More information", e -> {
      // TODO: Dialog should display all the different suggestions
    });
  }

  @Override
  protected String getPopupShellTitle() {
    return "SonarLint Multiple Connection Suggestions found";
  }

  @Override
  protected Image getPopupShellImage(int maximumHeight) {
    return SonarLintImages.BALLOON_IMG;
  }

  @Override
  protected String getMessage() {
    return "The local project '" + configScopeId + "' can be connected based on different suggestions. Click 'More "
      + "Information' to see them all. Do you want to choose which suggestion to use and then connect and bind the "
      + "project?";
  }

  @Override
  protected void createContentArea(Composite composite) {
    super.createContentArea(composite);

    addLinkWithTooltip("Connect", "Connect to organization", e -> {
      close();

      // user has to choose the suggestion first

      // AssistSuggestConnectionJob and afterwards bind all projects
    });

    addMoreInformationLink();
    addDontAskAgainLink();
  }
}
