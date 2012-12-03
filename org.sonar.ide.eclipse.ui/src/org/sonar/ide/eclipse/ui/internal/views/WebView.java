/*
 * Sonar Eclipse
 * Copyright (C) 2010-2012 SonarSource
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
import org.eclipse.swt.widgets.Control;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ide.api.SourceCode;
import org.sonar.ide.eclipse.core.internal.Messages;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.core.resources.ISonarResource;
import org.sonar.ide.eclipse.ui.internal.EclipseSonar;
import org.sonar.ide.eclipse.ui.internal.ISonarConstants;
import org.sonar.ide.eclipse.ui.internal.SonarUrls;
import org.sonar.wsclient.Host;

/**
 * @author Evgeny Mandrikov
 */
public class WebView extends AbstractSonarInfoView {
  private static final Logger LOG = LoggerFactory.getLogger(WebView.class);

  public static final String ID = ISonarConstants.PLUGIN_ID + ".views.WebView";

  private Browser browser;

  @Override
  protected Control getControl() {
    return browser;
  }

  @Override
  protected void internalCreatePartControl(Composite parent) {
    browser = new Browser(parent, SWT.NONE);
  }

  /**
   * @param input ISonarResource to be showin in the view
   */
  @Override
  protected void doSetInput(Object input) {
    ISonarResource sonarResource = (ISonarResource) input;
    SonarProject sonarProject = SonarProject.getInstance(sonarResource.getProject());
    String url = new SonarUrls().resourceUrl(sonarResource);
    Host host = SonarCorePlugin.getServersManager().findServer(sonarProject.getUrl());
    if (host == null) {
      browser.setText(NLS.bind(Messages.No_matching_server_in_configuration_for_project, sonarProject.getProject().getName(), url));
      return;
    }

    SourceCode sourceCode = EclipseSonar.getInstance(sonarResource.getProject()).search(sonarResource);
    if (sourceCode == null) {
      browser.setText("Not found.");
      return;
    }
    LOG.debug("Opening url {} in web view", url);

    if (host.getUsername() != null) {
      String userpwd = host.getUsername() + ":" + host.getPassword();
      byte[] encodedBytes = Base64.encodeBase64(userpwd.getBytes());
      browser.setUrl(url, null, new String[] {"Authorization: Basic " + new String(encodedBytes)});
    }
    else {
      browser.setUrl(url);
    }
  }
}
