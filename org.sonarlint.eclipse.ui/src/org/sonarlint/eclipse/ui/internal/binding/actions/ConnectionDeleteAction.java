/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2022 SonarSource SA
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
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.sonarlint.eclipse.core.internal.engine.connected.IConnectedEngineFacade;
import org.sonarlint.eclipse.ui.internal.Messages;
import org.sonarlint.eclipse.ui.internal.binding.DeleteConnectionDialog;

public class ConnectionDeleteAction extends SelectionProviderAction {
  private List<IConnectedEngineFacade> selectedServers;
  private Shell shell;

  public ConnectionDeleteAction(Shell shell, ISelectionProvider selectionProvider) {
    super(selectionProvider, Messages.actionDelete);
    this.shell = shell;
    var sharedImages = PlatformUI.getWorkbench().getSharedImages();
    setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
    setActionDefinitionId(IWorkbenchCommandConstants.EDIT_DELETE);
  }

  @Override
  public void selectionChanged(IStructuredSelection sel) {
    if (sel.isEmpty()) {
      setEnabled(false);
      return;
    }
    selectedServers = new ArrayList<>();
    var iterator = sel.iterator();
    while (iterator.hasNext()) {
      var obj = iterator.next();
      if (obj instanceof IConnectedEngineFacade) {
        var server = (IConnectedEngineFacade) obj;
        selectedServers.add(server);
      } else {
        setEnabled(false);
        return;
      }
    }
    setEnabled(!selectedServers.isEmpty());
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
    if (selectedServers == null) {
      var sel = getStructuredSelection();
      if (sel != null) {
        selectionChanged(sel);
      }
    }

    if (selectedServers != null && !selectedServers.isEmpty()) {
      // No check is made for valid parameters at this point, since if there is a failure, it
      // should be output to the error log instead of failing silently.
      var dialog = new DeleteConnectionDialog(shell, selectedServers);
      dialog.open();
    }
  }

}
