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

import org.apache.commons.lang.StringUtils;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.servers.SonarServer;

public class EditServerLocationWizard extends AbstractServerLocationWizard {

  private final SonarServer sonarServer;

  public EditServerLocationWizard(SonarServer sonarServer) {
    super(new ServerLocationWizardPage(sonarServer), "Edit SonarQube Server");
    this.sonarServer = sonarServer;
  }

  @Override
  protected void doFinish(String id, String serverUrl, String username, String password) {
    String oldServerId = sonarServer.getId();
    if (StringUtils.isNotBlank(oldServerId) && (SonarCorePlugin.getServersManager().findServer(oldServerId) != null)) {
      SonarCorePlugin.getServersManager().removeServer(sonarServer);
    }
    super.doFinish(id, serverUrl, username, password);
  }
}
