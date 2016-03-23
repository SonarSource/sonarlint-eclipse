/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.server.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.ui.internal.Messages;
import org.sonarlint.eclipse.ui.internal.server.wizard.EditServerLocationWizard;

public class ServerEditAction extends SelectionProviderAction {
  private List<IServer> servers;
  private Shell shell;

  public ServerEditAction(Shell shell, ISelectionProvider selectionProvider) {
    super(selectionProvider, Messages.actionEdit);
    this.shell = shell;
    setActionDefinitionId(IWorkbenchCommandConstants.FILE_RENAME);
  }

  @Override
  public void selectionChanged(IStructuredSelection sel) {
    if (sel.isEmpty()) {
      setEnabled(false);
      return;
    }
    servers = new ArrayList<>();
    Iterator iterator = sel.iterator();
    while (iterator.hasNext()) {
      Object obj = iterator.next();
      if (obj instanceof IServer) {
        IServer server = (IServer) obj;
        servers.add(server);
      } else {
        setEnabled(false);
        return;
      }
    }
    setEnabled(servers.size() == 1);
  }

  @Override
  public void run() {
    // It is possible that the server is created and added to the server view on workbench
    // startup. As a result, when the user switches to the server view, the server is
    // selected, but the selectionChanged event is not called, which results in servers
    // being null. When servers is null the server will not be deleted and the error log
    // will have an IllegalArgumentException.
    //
    // To handle the case where servers is null, the selectionChanged method is called
    // to ensure servers will be populated.
    if (servers == null) {

      IStructuredSelection sel = getStructuredSelection();
      if (sel != null) {
        selectionChanged(sel);
      }
    }

    if (servers != null && !servers.isEmpty()) {

      openEditWizard(shell, servers.get(0));
    }
  }

  public static void openEditWizard(Shell shell, IServer server) {
    IWorkbench workbench = PlatformUI.getWorkbench();
    IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();
    ISelection selection = workbenchWindow.getSelectionService().getSelection();

    IStructuredSelection selectionToPass;
    if (selection instanceof IStructuredSelection) {
      selectionToPass = (IStructuredSelection) selection;
    } else {
      selectionToPass = StructuredSelection.EMPTY;
    }
    EditServerLocationWizard wizard = new EditServerLocationWizard(server);
    wizard.init(workbench, selectionToPass);
    WizardDialog dialog = new WizardDialog(shell, wizard);
    dialog.open();
  }

}
