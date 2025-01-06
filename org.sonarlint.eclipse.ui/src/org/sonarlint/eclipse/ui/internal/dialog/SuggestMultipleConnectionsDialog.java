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

import java.util.List;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.rpc.protocol.client.connection.ConnectionSuggestionDto;

/**
 *  Dialog shown when invoked from the specific notification pop-up. It will contain the mapping of suggestions for the
 *  specific project. This is not beautifully designed as it is an corner case, it should be just informative!
 */
public class SuggestMultipleConnectionsDialog extends Dialog {
  private final ISonarLintProject project;
  private final List<ConnectionSuggestionDto> suggestions;

  public SuggestMultipleConnectionsDialog(Shell parentShell, ISonarLintProject project, List<ConnectionSuggestionDto> suggestions) {
    super(parentShell);
    this.project = project;
    this.suggestions = suggestions;
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    var container = (Composite) super.createDialogArea(parent);
    container.setLayout(new GridLayout(1, true));

    var tree = new Tree(container, SWT.BORDER);
    tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    for (var suggestion : suggestions) {
      var projectKey = "Remote project '";
      String title;
      if (suggestion.getConnectionSuggestion().isLeft()) {
        var sonarQubeSuggestion = suggestion.getConnectionSuggestion().getLeft();
        projectKey += sonarQubeSuggestion.getProjectKey();
        title = "SonarQube Server at '" + sonarQubeSuggestion.getServerUrl() + "':";
      } else {
        var sonarCloudSuggestion = suggestion.getConnectionSuggestion().getRight();
        projectKey += sonarCloudSuggestion.getProjectKey();
        title = "SonarQube Cloud organization '" + sonarCloudSuggestion.getOrganization() + "':";
      }
      projectKey += "'";

      var titleTreeItem = new TreeItem(tree, SWT.NONE);
      titleTreeItem.setText(title);
      var projectKeyTreeItem = new TreeItem(titleTreeItem, SWT.NONE);
      projectKeyTreeItem.setText(projectKey);
    }

    return container;
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);

    newShell.setText("Connection suggestions for Eclipse project '" + project.getName() + "'");
    newShell.setMinimumSize(600, 300);
  }

  @Override
  protected boolean isResizable() {
    return true;
  }
}
