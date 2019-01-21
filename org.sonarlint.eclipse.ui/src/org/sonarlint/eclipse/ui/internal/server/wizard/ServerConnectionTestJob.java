/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2019 SonarSource SA
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

import javax.annotation.Nullable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.sonarlint.eclipse.core.internal.server.Server;

final class ServerConnectionTestJob implements IRunnableWithProgress {

  private IStatus status;
  private final String url;
  private final String username;
  private final String password;
  private final String organization;

  ServerConnectionTestJob(String url, @Nullable String organization, String username, String password) {
    this.url = url;
    this.organization = organization;
    this.username = username;
    this.password = password;
  }

  @Override
  public void run(IProgressMonitor monitor) {
    String msg;
    if (organization != null) {
      msg = "Testing access to the organization";
    } else {
      msg = "Testing connection";
    }
    monitor.beginTask(msg, IProgressMonitor.UNKNOWN);
    try {
      status = Server.testConnection(url, organization, username, password);
    } finally {
      monitor.done();
    }
  }

  public IStatus getStatus() {
    return status;
  }
}
