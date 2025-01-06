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
package org.sonarlint.eclipse.ui.internal.binding.actions;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.sonarlint.eclipse.ui.internal.Messages;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.binding.UnbindProjectDialog;

/** Action corresponding to the context menu option to unbind a project */
public class ProjectUnbindAction extends AbstractBindingAction {
  public ProjectUnbindAction(Shell shell, ISelectionProvider selectionProvider) {
    super(shell, selectionProvider, Messages.actionUnbind, SonarLintImages.UNBIND);
    setActionDefinitionId(IWorkbenchCommandConstants.EDIT_DELETE);
  }

  @Override
  protected boolean disable(IStructuredSelection sel) {
    return sel.isEmpty();
  }

  @Override
  protected void doRun() {
    var dialog = new UnbindProjectDialog(shell, selectedProjects);
    dialog.open();
  }
}
