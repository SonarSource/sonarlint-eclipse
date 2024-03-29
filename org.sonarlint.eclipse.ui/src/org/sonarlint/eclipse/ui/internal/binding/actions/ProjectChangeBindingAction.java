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
package org.sonarlint.eclipse.ui.internal.binding.actions;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.Messages;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.binding.wizard.project.ProjectBindingWizard;

public class ProjectChangeBindingAction extends SelectionProviderAction {
  private final Shell shell;
  private List<ISonarLintProject> selectedProjects;

  public ProjectChangeBindingAction(Shell shell, ISelectionProvider selectionProvider) {
    super(selectionProvider, Messages.actionChangeBinding);
    this.shell = shell;
    setImageDescriptor(SonarLintImages.SYNCED_IMG);
  }

  @Override
  public void selectionChanged(IStructuredSelection sel) {
    if (sel.isEmpty()) {
      setEnabled(false);
      return;
    }
    selectedProjects = new ArrayList<>();
    var enabled = false;
    var iterator = sel.iterator();
    while (iterator.hasNext()) {
      var obj = iterator.next();
      if (obj instanceof ISonarLintProject) {
        var project = (ISonarLintProject) obj;
        selectedProjects.add(project);
        enabled = true;
      } else {
        setEnabled(false);
        return;
      }
    }
    setEnabled(enabled);
  }

  @Override
  public void run() {
    // It is possible that the project is created and added to the connection view on workbench
    // startup. As a result, when the user switches to the connection view, the project is
    // selected, but the selectionChanged event is not called, which results in selectedProjects
    // being null. When selectedProjects is null the project will not be processed
    //
    // To handle the case where selectedProjects is null, the selectionChanged method is called
    // to ensure selectedProjects will be populated.
    if (selectedProjects == null) {
      var sel = getStructuredSelection();
      if (sel != null) {
        selectionChanged(sel);
      }
    }

    if (selectedProjects != null) {
      final var dialog = ProjectBindingWizard.createDialog(shell, selectedProjects);
      dialog.open();
    }
  }

}
