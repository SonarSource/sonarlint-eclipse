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

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectionFacade;
import org.sonarlint.eclipse.ui.internal.Messages;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.ServerConnectionWizard;

public class ConnectionEditAction extends AbstractConnectionAction {

  public ConnectionEditAction(Shell shell, ISelectionProvider selectionProvider) {
    super(Messages.actionEdit, SonarLintImages.EDIT_SERVER, shell, selectionProvider);
    setActionDefinitionId(IWorkbenchCommandConstants.FILE_RENAME);
  }

  @Override
  protected void doRun(Shell shell, ConnectionFacade selectedConnection) {
    openEditWizard(shell, selectedConnection);
  }

  public static void openEditWizard(Shell shell, ConnectionFacade connection) {
    var dialog = ServerConnectionWizard.createDialog(shell, connection);
    dialog.open();
  }

}
