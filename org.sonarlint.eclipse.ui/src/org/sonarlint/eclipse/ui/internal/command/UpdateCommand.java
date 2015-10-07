/*
 * SonarLint for Eclipse
 * Copyright (C) 2015 SonarSource
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
package org.sonarlint.eclipse.ui.internal.command;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;

public class UpdateCommand extends AbstractHandler {

  private static final String TITLE = "SonarLint update";

  @Override
  public Object execute(final ExecutionEvent event) throws ExecutionException {
    new Job("Update SonarLint") {

      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          SonarLintCorePlugin.getDefault().getRunner().tryUpdate();
        } catch (final Exception e) {
          Display.getDefault().syncExec(new Runnable() {
            public void run() {
              MessageDialog.openError(HandlerUtil.getActiveShell(event).getShell(), TITLE, "Unable to update SonarLint: " + e.getMessage());
            }
          });
          return Status.OK_STATUS;
        }
        final String version = SonarLintCorePlugin.getDefault().getRunner().getVersion();
        Display.getDefault().syncExec(new Runnable() {
          public void run() {
            if (version == null) {
              MessageDialog.openError(HandlerUtil.getActiveShell(event).getShell(), TITLE, "Unable to update SonarLint. Please check logs in SonarLint console.");
            } else {
              MessageDialog.openInformation(HandlerUtil.getActiveShell(event).getShell(), TITLE, "SonarLint is up to date and running");
            }
          }
        });

        return Status.OK_STATUS;
      }
    }.schedule();
    return null;
  }

}
