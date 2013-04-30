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
package org.sonar.ide.eclipse.ui.internal.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.core.resources.ISonarResource;
import org.sonar.ide.eclipse.ui.internal.views.WebView;

/**
 * Open the internal web browser to show the page of the sonar server corresponding to the selection.
 *
 * @author Jérémie Lagarde
 */
public class OpenInBrowserAction implements IObjectActionDelegate {

  private static final Logger LOG = LoggerFactory.getLogger(OpenInBrowserAction.class);

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
    try {
      PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(WebView.ID);
    } catch (PartInitException e) {
      LOG.error("Unable to open Web View", e);
    }
  }

  public void selectionChanged(IAction action, ISelection selection) {
    if (selection instanceof IStructuredSelection) {
      this.selection = (IStructuredSelection) selection;
    }
  }
}
