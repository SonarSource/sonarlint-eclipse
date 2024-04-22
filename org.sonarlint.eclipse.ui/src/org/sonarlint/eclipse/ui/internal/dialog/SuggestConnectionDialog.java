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
package org.sonarlint.eclipse.ui.internal.dialog;

import java.util.List;
import java.util.Map;
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
import org.sonarlint.eclipse.ui.internal.binding.ProjectSuggestionDto;
import org.sonarsource.sonarlint.core.rpc.protocol.common.Either;

/**
 *  Dialog shown when invoked from the specific notification pop-up. It is only displayed if there is more than one
 *  remote project for a connection or only one remote project but multiple local Eclipse projects - this information
 *  just cannot be shown in the notification pop-up itself. This is not beautifully designed as it is an corner case,
 *  it should be just informative!
 */
public class SuggestConnectionDialog extends Dialog {
  private final Either<String, String> serverUrlOrOrganization;
  private final Map<String, List<ProjectSuggestionDto>> projectMapping;

  public SuggestConnectionDialog(Shell parentShell, Either<String, String> serverUrlOrOrganization,
    Map<String, List<ProjectSuggestionDto>> projectMapping) {
    super(parentShell);
    this.serverUrlOrOrganization = serverUrlOrOrganization;
    this.projectMapping = projectMapping;
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    var container = (Composite) super.createDialogArea(parent);
    container.setLayout(new GridLayout(1, true));

    var tree = new Tree(container, SWT.BORDER);
    tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    for (var entry : projectMapping.entrySet()) {
      var projectKeyTreeItem = new TreeItem(tree, SWT.NONE);
      projectKeyTreeItem.setText("Remote project '" + entry.getKey() + "'");
      for (var projectSuggestion : entry.getValue()) {
        var projectTreeItem = new TreeItem(projectKeyTreeItem, SWT.NONE);
        projectTreeItem.setText("Eclipse project '" + projectSuggestion.getProject().getName() + "'");
      }
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

    if (serverUrlOrOrganization.isLeft()) {
      newShell.setText("Connection suggestions for SonarQube at '" + serverUrlOrOrganization.getLeft() + "'");
    } else {
      newShell.setText("Connection suggestions for SonarCloud organization '" + serverUrlOrOrganization.getRight() + "'");
    }
    newShell.setMinimumSize(600, 300);
  }

  @Override
  protected boolean isResizable() {
    return true;
  }
}
