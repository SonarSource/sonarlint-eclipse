/*
 * Sonar Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * dev@sonar.codehaus.org
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
package org.sonar.ide.eclipse.ui.internal.views;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.sonar.ide.eclipse.common.servers.ISonarServer;
import org.sonar.ide.eclipse.core.internal.Messages;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;

/**
 * Open Sonar server URL in an embedded browser
 */
public abstract class AbstractSonarWebView extends ViewPart {

  private Browser browser;

  @Override
  public void createPartControl(Composite parent) {
    browser = new Browser(parent, SWT.NONE);
  }

  @Override
  public final void setFocus() {
    browser.setFocus();
  }

  protected Browser getBrowser() {
    return browser;
  }

  protected void open(SonarProject sonarProject, String url) {
    ISonarServer sonarServer = SonarCorePlugin.getServersManager().findServer(sonarProject.getUrl());
    if (sonarServer == null) {
      browser.setText(NLS.bind(Messages.No_matching_server_in_configuration_for_project, sonarProject.getProject().getName(), url));
      return;
    }

    if (sonarServer.getUsername() != null) {
      String userpwd = sonarServer.getUsername() + ":" + sonarServer.getPassword();
      byte[] encodedBytes = Base64.encodeBase64(userpwd.getBytes());
      browser.setUrl(url, null, new String[] {"Authorization: Basic " + new String(encodedBytes)});
    }
    else {
      browser.setUrl(url);
    }
  }

}
