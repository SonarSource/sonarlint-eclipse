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
package org.sonar.ide.eclipse.ui.internal.wizards;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;

public abstract class AbstractServerLocationWizard extends Wizard implements INewWizard {

  private final ServerLocationWizardPage page;

  public AbstractServerLocationWizard(ServerLocationWizardPage page, String title) {
    super();
    setNeedsProgressMonitor(true);
    setWindowTitle(title);
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
    final String username = page.getUsername();
    final String password = page.getPassword();

    IRunnableWithProgress op = new IRunnableWithProgress() {
      @Override
      public void run(IProgressMonitor monitor) throws InvocationTargetException {
        monitor.beginTask("Saving " + serverId, 1);
        try {
          doFinish(serverId, serverUrl, username, password);
        } catch (Exception e) {
          SonarCorePlugin.getDefault().error(e.getMessage(), e);
        } finally {
          monitor.done();
        }
      }
    };
    try {
      getContainer().run(true, false, op);
    } catch (InterruptedException e) {
      return false;
    } catch (InvocationTargetException e) {
      Throwable realException = e.getTargetException();
      MessageDialog.openError(getShell(), "Error", realException.getMessage());
      return false;
    }
    return true;
  }

  protected void doFinish(String serverId, String serverUrl, String username, String password) {
    SonarCorePlugin.getServersManager().addServer(SonarCorePlugin.getServersManager().create(serverId, serverUrl, username, password));
  }
}
