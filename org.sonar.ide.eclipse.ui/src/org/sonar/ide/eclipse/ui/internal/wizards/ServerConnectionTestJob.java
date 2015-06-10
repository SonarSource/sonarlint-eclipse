/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.ui.internal.wizards;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.sonar.ide.eclipse.common.servers.ISonarServer;
import org.sonar.ide.eclipse.core.SonarEclipseException;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.ui.internal.Messages;
import org.sonar.ide.eclipse.ui.internal.SonarUiPlugin;
import org.sonar.ide.eclipse.wsclient.ISonarWSClientFacade.ConnectionTestResult;
import org.sonar.ide.eclipse.wsclient.WSClientFactory;

final class ServerConnectionTestJob implements IRunnableWithProgress {

  private IStatus status;

  private final String username;
  private final String password;
  private final String serverUrl;

  ServerConnectionTestJob(String username, String password, String serverUrl) {
    this.username = username;
    this.password = password;
    this.serverUrl = serverUrl;
  }

  @Override
  public void run(IProgressMonitor monitor) {
    monitor.beginTask("Testing", IProgressMonitor.UNKNOWN);
    try {
      ISonarServer newServer = SonarCorePlugin.getServersManager().create(serverUrl, username, password);
      ConnectionTestResult result = WSClientFactory.getSonarClient(newServer).testConnection();
      switch (result.status) {
        case OK:
          status = new Status(IStatus.OK, SonarUiPlugin.PLUGIN_ID, Messages.ServerLocationWizardPage_msg_connected);
          break;
        case AUTHENTICATION_ERROR:
          status = new Status(IStatus.ERROR, SonarUiPlugin.PLUGIN_ID, Messages.ServerLocationWizardPage_msg_authentication_error);
          break;
        case CONNECT_ERROR:
          status = new Status(IStatus.ERROR, SonarUiPlugin.PLUGIN_ID, Messages.ServerLocationWizardPage_msg_connection_error + result.message);
          break;
        default:
          throw new SonarEclipseException("Unknow status code: " + result);
      }
    } finally {
      monitor.done();
    }
  }

  public IStatus getStatus() {
    return status;
  }
}
