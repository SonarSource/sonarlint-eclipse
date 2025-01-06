/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.dialog;

import java.util.HashMap;
import java.util.List;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.rpc.protocol.client.connection.ConnectionSuggestionDto;

/** Dialog shown when there are multiple connection suggestions for one specific Eclipse project. */
public class SuggestMultipleConnectionSelectionDialog extends ElementListSelectionDialog {
  private final HashMap<String, ConnectionSuggestionDto> localMapping;

  public SuggestMultipleConnectionSelectionDialog(Shell parent, ISonarLintProject project, List<ConnectionSuggestionDto> suggestions) {
    super(parent, new LabelProvider());
    setAllowDuplicates(false);
    setMultipleSelection(false);

    setTitle("Choose suggestion for Eclipse project '" + project.getName() + "'");

    localMapping = new HashMap<>();
    for (var suggestion : suggestions) {
      localMapping.put(convertSuggestionToString(suggestion), suggestion);
    }
    setElements(localMapping.keySet().toArray());
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setMinimumSize(600, 300);
  }

  private static String convertSuggestionToString(ConnectionSuggestionDto suggestion) {
    var projectInfo = " with remote project '";
    String connectionInfo;
    if (suggestion.getConnectionSuggestion().isLeft()) {
      var sonarQubeSuggestion = suggestion.getConnectionSuggestion().getLeft();
      projectInfo += sonarQubeSuggestion.getProjectKey();
      connectionInfo = "SonarQube Server at '" + sonarQubeSuggestion.getServerUrl() + "'";
    } else {
      var sonarCloudSuggestion = suggestion.getConnectionSuggestion().getRight();
      projectInfo += sonarCloudSuggestion.getProjectKey();
      connectionInfo = "SonarQube Cloud organization '" + sonarCloudSuggestion.getOrganization() + "'";
    }
    projectInfo += "'";

    return connectionInfo + projectInfo;
  }

  public ConnectionSuggestionDto getSuggestionFromElement(String element) {
    return localMapping.get(element);
  }
}
