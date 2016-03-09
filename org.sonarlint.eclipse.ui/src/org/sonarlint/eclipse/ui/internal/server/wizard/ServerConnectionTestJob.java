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
package org.sonarlint.eclipse.ui.internal.server.wizard;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.sonarlint.eclipse.core.internal.server.IServer;

final class ServerConnectionTestJob implements IRunnableWithProgress {

  private IStatus status;
  private final IServer server;
  private final String username;
  private final String password;

  ServerConnectionTestJob(IServer server, String username, String password) {
    this.server = server;
    this.username = username;
    this.password = password;
  }

  @Override
  public void run(IProgressMonitor monitor) {
    monitor.beginTask("Testing", IProgressMonitor.UNKNOWN);
    try {
      status = server.testConnection(username, password);
    } finally {
      monitor.done();
    }
  }

  public IStatus getStatus() {
    return status;
  }
}
