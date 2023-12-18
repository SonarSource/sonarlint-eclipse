/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2023 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.binding.assist;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;
import org.sonarlint.eclipse.core.internal.engine.connected.IConnectedEngineFacade;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.ServerConnectionModel;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.ServerConnectionModel.ConnectionType;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.ServerConnectionWizard;
import org.sonarlint.eclipse.ui.internal.util.DisplayUtils;

public class AssistCreatingConnectionJob extends UIJob {

  private final String serverUrl;
  @Nullable
  private String connectionId;

  public AssistCreatingConnectionJob(String serverUrl) {
    super("Assist creating SonarLint connection");
    // We don't want to have this job visible to the user, as there should be a dialog anyway
    setSystem(true);
    this.serverUrl = serverUrl;
  }

  @Override
  public IStatus runInUIThread(IProgressMonitor monitor) {
    var shell = DisplayUtils.bringToFront();
    var dialog = new ConfirmConnectionCreationDialog(shell, serverUrl);
    var result = dialog.open();
    if (result != 0) {
      return Status.CANCEL_STATUS;
    }
    var connection = createConnection(serverUrl);
    if (connection != null) {
      this.connectionId = connection.getId();
      return Status.OK_STATUS;
    }

    return Status.CANCEL_STATUS;
  }

  @Nullable
  private static IConnectedEngineFacade createConnection(String serverUrl) {
    var model = new ServerConnectionModel();
    model.setConnectionType(ConnectionType.ONPREMISE);
    model.setServerUrl(serverUrl);
    var wizard = new ServerConnectionWizard(model);
    wizard.setSkipBindingWizard(true);
    var dialog = ServerConnectionWizard.createDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), wizard);
    dialog.setBlockOnOpen(true);
    dialog.open();
    return wizard.getResultServer();
  }

  @Nullable
  public String getConnectionId() {
    return connectionId;
  }

}
