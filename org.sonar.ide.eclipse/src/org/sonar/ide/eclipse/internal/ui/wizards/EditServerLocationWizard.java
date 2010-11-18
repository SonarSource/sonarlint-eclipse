/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.internal.ui.wizards;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.INewWizard;
import org.sonar.ide.eclipse.ui.SonarUiPlugin;
import org.sonar.wsclient.Host;

public class EditServerLocationWizard extends AbstractServerLocationWizard implements INewWizard {

  private Host host;

  public EditServerLocationWizard(Host server) {
    super(new ServerLocationWizardPage(server), "Edit Sonar Server");
    this.host = server;
  }

  protected void doFinish(String serverUrl, String username, String password, IProgressMonitor monitor) throws Exception {
    String oldServerUrl = host.getHost();
    if (StringUtils.isNotBlank(oldServerUrl) && SonarUiPlugin.getServerManager().findServer(oldServerUrl) != null) {
      SonarUiPlugin.getServerManager().removeServer(oldServerUrl);
    }
    super.doFinish(serverUrl, username, password, monitor);
  }
}
