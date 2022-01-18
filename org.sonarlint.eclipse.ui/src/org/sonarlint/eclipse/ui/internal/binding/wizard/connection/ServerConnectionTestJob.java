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
package org.sonarlint.eclipse.ui.internal.binding.wizard.connection;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectedEngineFacade;

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
    var msg = organization == null ? "Testing connection" : "Testing access to the organization";
    monitor.beginTask(msg, IProgressMonitor.UNKNOWN);
    try {
      var futureStatus = ConnectedEngineFacade.testConnection(url, organization, username, password);
      while (!futureStatus.isDone()) {
        Thread.sleep(500);
        if (monitor.isCanceled()) {
          futureStatus.cancel(true);
        }
      }
      status = futureStatus.get();
    } catch (CancellationException e) {
      // happen if get() is called while future was canceled by user
      status = new Status(IStatus.CANCEL, SonarLintCorePlugin.PLUGIN_ID, "Canceled by user");
    } catch (InterruptedException | ExecutionException e) {
      // should not happen, as we check if the future is done
      status = new Status(IStatus.ERROR, SonarLintCorePlugin.PLUGIN_ID, "Network error");
    } finally {
      monitor.done();
    }
  }

  public IStatus getStatus() {
    return status;
  }
}
