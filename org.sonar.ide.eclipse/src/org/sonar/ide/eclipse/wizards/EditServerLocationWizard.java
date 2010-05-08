/*
 * Copyright (C) 2010 Evgeny Mandrikov
 *
 * Sonar-IDE is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar-IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar-IDE; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.wizards;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.INewWizard;
import org.sonar.ide.eclipse.Messages;
import org.sonar.ide.eclipse.SonarPlugin;

/**
 * @author Jérémie Lagarde
 */
public class EditServerLocationWizard extends AbstractServerLocationWizard implements INewWizard {

  private String oldServerUrl;
  
  public EditServerLocationWizard(String serverUrl) {
    super();
    oldServerUrl = serverUrl;
  }

  protected String getTitle() {
    return Messages.getString("action.edit.server.desc"); //$NON-NLS-1$
  }

  protected String getDefaultUrl() {
    return oldServerUrl;
  }
  
  protected void doFinish(String serverUrl, String username, String password, IProgressMonitor monitor) throws Exception {
    if(StringUtils.isNotBlank(oldServerUrl) && SonarPlugin.getServerManager().findServer(oldServerUrl)!= null )
    SonarPlugin.getServerManager().removeServer(oldServerUrl);
    super.doFinish(serverUrl, username, password,monitor);
  }
}
