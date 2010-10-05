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

package org.sonar.ide.eclipse.views;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.sonar.ide.api.SourceCode;
import org.sonar.ide.eclipse.core.ISonarResource;
import org.sonar.ide.eclipse.internal.EclipseSonar;
import org.sonar.ide.eclipse.properties.ProjectProperties;
import org.sonar.ide.eclipse.ui.AbstractSonarInfoView;

/**
 * @author Evgeny Mandrikov
 */
public class WebView extends AbstractSonarInfoView {

  public static final String ID = "org.sonar.ide.eclipse.views.WebView";

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
    SourceCode sourceCode = EclipseSonar.getInstance(sonarResource.getProject()).search(sonarResource);
    if (sourceCode == null) {
      browser.setText("Not found.");
      return;
    }
    ProjectProperties properties = ProjectProperties.getInstance(sonarResource.getProject());
    StringBuffer url = new StringBuffer(properties.getUrl()).append("/resource/index/").append(sourceCode.getKey());
    if (sonarResource.getResource() instanceof IFile) {
      url.append("?metric=coverage");
    } else {
      url.append("?page=dashboard");
    }
    browser.setUrl(url.toString());
  }
}
