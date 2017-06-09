/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2017 SonarSource SA
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

import java.lang.reflect.InvocationTargetException;
import javax.annotation.Nullable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;

public abstract class AbstractServerLocationWizard extends Wizard implements INewWizard {

  private final ServerLocationWizardPage page;

  public AbstractServerLocationWizard(ServerLocationWizardPage page, String title) {
    super();
    setNeedsProgressMonitor(true);
    setWindowTitle(title);
    setHelpAvailable(false);
    this.page = page;
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
  }

  @Override
  public void addPages() {
    addPage(page);
  }

  @Override
  public boolean performFinish() {
    final String serverId = page.getServerId();
    final String serverUrl = page.getServerUrl();
    final String organization = StringUtils.trimToNull(page.getOrganization());
    final String username = page.getUsername();
    final String password = page.getPassword();
    IRunnableWithProgress op = monitor -> {
      monitor.beginTask("Saving '" + serverId + "'", 1);
      try {
        doFinish(serverId, serverUrl, organization, username, password);
      } finally {
        monitor.done();
      }
    };
    try {
      getContainer().run(true, false, op);
    } catch (InterruptedException e) {
      return false;
    } catch (InvocationTargetException e) {
      Throwable realException = e.getCause();
      // show error dialog
      ErrorDialog.openError(getShell(), "Error", "Error when editing server configuration",
        new Status(IStatus.ERROR, SonarLintUiPlugin.PLUGIN_ID, realException.getLocalizedMessage(), realException));
      return false;
    }
    return true;
  }

  protected abstract void doFinish(String serverId, String url, @Nullable String organization, String username, String password);
}
