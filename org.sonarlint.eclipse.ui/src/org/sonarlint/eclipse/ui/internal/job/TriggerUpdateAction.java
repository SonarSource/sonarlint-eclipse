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
package org.sonarlint.eclipse.ui.internal.job;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.progress.UIJob;
import org.sonarlint.eclipse.core.SonarLintLogger;

/**
 *  This is more or less copied from {@link org.eclipse.platform.internal.LaunchUpdateIntroAction} in order to trigger
 *  an update inside the IDE, relying on Eclipse Equinox p2!
 */
public class TriggerUpdateAction extends UIJob {
  private static final String COMMAND_P2 = "org.eclipse.equinox.p2.ui.sdk.update";
  private static final String COMMAND_UPDATE_MANAGER = "org.eclipse.ui.update.findAndInstallUpdates";

  public TriggerUpdateAction() {
    super("Triggering Eclipse Equinox p2 update for the IDE ...");
  }

  @Override
  public IStatus runInUIThread(IProgressMonitor monitor) {
    var commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
    var handlerService = PlatformUI.getWorkbench().getService(IHandlerService.class);

    var ret = runCommand(commandService, handlerService, COMMAND_P2);
    if (!ret) {
      ret = runCommand(commandService, handlerService, COMMAND_UPDATE_MANAGER);
    }
    return ret ? Status.OK_STATUS : Status.CANCEL_STATUS;
  }

  public boolean runCommand(ICommandService commandService, IHandlerService handlerService, String command) {
    var cmd = commandService.getCommand(command);
    var event = handlerService.createExecutionEvent(cmd, null);
    try {
      cmd.executeWithChecks(event);
    } catch (Exception err) {
      SonarLintLogger.get().error("Cannot invoke '" + command + "' to check for updates in the IDE.", err);
      return false;
    }
    return true;
  }
}
