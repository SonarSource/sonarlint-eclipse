/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2012 SonarSource
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
package org.sonar.ide.eclipse.internal.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.core.ISonarResource;
import org.sonar.ide.eclipse.internal.SonarUrls;
import org.sonar.ide.eclipse.internal.core.resources.ProjectProperties;
import org.sonar.ide.eclipse.ui.SonarUiPlugin;

import java.net.URL;

/**
 * Open the internal web browser to show the page of the sonar server corresponding to the selection.
 * 
 * @author Jérémie Lagarde
 */
public class OpenInBrowserAction implements IObjectActionDelegate {

  private IStructuredSelection selection;

  public OpenInBrowserAction() {
    super();
  }

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
  }

  public void run(IAction action) {
    try {
      Object element = selection.getFirstElement();
      if (element instanceof ISonarResource) {
        openBrowser((ISonarResource) element);
      }
    } catch (Exception e) {
      LoggerFactory.getLogger(getClass()).error(e.getMessage(), e);
    }
  }

  protected void openBrowser(ISonarResource sonarResource) {
    IWorkbenchBrowserSupport browserSupport = SonarUiPlugin.getDefault().getWorkbench().getBrowserSupport();
    ProjectProperties properties = ProjectProperties.getInstance(sonarResource.getProject());
    try {
      String url = new SonarUrls().resourceUrl(sonarResource);

      URL consoleURL = new URL(url);
      if (browserSupport.isInternalWebBrowserAvailable()) {
        browserSupport.createBrowser("id" + properties.getUrl().hashCode()).openURL(consoleURL);
      } else {
        browserSupport.getExternalBrowser().openURL(consoleURL);
      }
    } catch (Exception e) {
      LoggerFactory.getLogger(getClass()).error(e.getMessage(), e);
    }
  }

  public void selectionChanged(IAction action, ISelection selection) {
    if (selection instanceof IStructuredSelection) {
      this.selection = (IStructuredSelection) selection;
    }
  }
}
