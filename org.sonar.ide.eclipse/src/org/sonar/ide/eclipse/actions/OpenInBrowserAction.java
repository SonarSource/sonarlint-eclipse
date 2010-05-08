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

package org.sonar.ide.eclipse.actions;

import java.net.URL;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.properties.ProjectProperties;
import org.sonar.ide.eclipse.utils.EclipseResourceUtils;

/**
 * Open the internal web browser to show the page of the sonar server corresponding to the selection.
 * 
 * @author Jérémie Lagarde
 * 
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
      if (element instanceof IResource) {
        openBrowser((IResource)element);
      }  
    } catch (Exception e) {
      SonarPlugin.getDefault().displayError(IStatus.ERROR, e.getMessage(), e, true);
    }
  }

  protected void openBrowser(IResource resource) {
    String fileKey = EclipseResourceUtils.getInstance().getFileKey(resource);
    IWorkbenchBrowserSupport browserSupport = SonarPlugin.getDefault().getWorkbench().getBrowserSupport();
    ProjectProperties properties = ProjectProperties.getInstance(resource);
    try {
      URL consoleURL = new URL(properties.getUrl() + "/resource/index/" + fileKey);
      if (browserSupport.isInternalWebBrowserAvailable()) {
        browserSupport.createBrowser("id" + properties.getUrl().hashCode()).openURL(consoleURL);
      } else {
        browserSupport.getExternalBrowser().openURL(consoleURL);
      }
    }catch(Exception e) {
      SonarPlugin.getDefault().displayError(IStatus.ERROR, e.getMessage(), e, true);
    }
  }
  
  public void selectionChanged(IAction action, ISelection selection) {
    if (selection instanceof IStructuredSelection) {
      this.selection = (IStructuredSelection) selection;
    }
  }
}
