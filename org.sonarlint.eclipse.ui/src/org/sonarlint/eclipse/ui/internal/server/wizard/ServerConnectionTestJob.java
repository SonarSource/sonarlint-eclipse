/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonarlint.eclipse.ui.internal.server.wizard;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;

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
      SonarLintCorePlugin.getDefault().testConnection(serverUrl, username, password);
      // ConnectionTestResult result = WSClientFactory.getSonarClient(newServer).testConnection();
      // switch (result.status) {
      // case OK:
      // status = new Status(IStatus.OK, SonarUiPlugin.PLUGIN_ID, Messages.ServerLocationWizardPage_msg_connected);
      // break;
      // case AUTHENTICATION_ERROR:
      // status = new Status(IStatus.ERROR, SonarUiPlugin.PLUGIN_ID, Messages.ServerLocationWizardPage_msg_authentication_error);
      // break;
      // case CONNECT_ERROR:
      // status = new Status(IStatus.ERROR, SonarUiPlugin.PLUGIN_ID, Messages.ServerLocationWizardPage_msg_connection_error +
      // result.message);
      // break;
      // default:
      // throw new SonarEclipseException("Unknow status code: " + result);
      // }
    } finally {
      monitor.done();
    }
  }

  public IStatus getStatus() {
    return status;
  }
}
