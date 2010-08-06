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

package org.sonar.ide.eclipse.compare;

import org.eclipse.compare.CompareUI;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.sonar.ide.eclipse.internal.EclipseSonar;
import org.sonar.ide.eclipse.utils.EclipseResourceUtils;
import org.sonar.wsclient.services.Source;

/**
 * @author Jérémie Lagarde
 */
public class CompareWithSonarAction implements IWorkbenchWindowActionDelegate {

  private IResource resource;

  public void dispose() {
    resource = null;
  }

  public void init(final IWorkbenchWindow window) {
  }

  public void selectionChanged(final IAction action, final ISelection selection) {
    resource = null;
    if (selection instanceof IResource) {
      resource = (IResource) selection;
    }
    if (selection instanceof IStructuredSelection) {
      resource = (IResource) ((IStructuredSelection) selection).getFirstElement();
    }
  }

  public void run(final IAction action) {
    if (resource != null) {
      final String resourceKey = EclipseResourceUtils.getInstance().getFileKey(resource);
      final Source source = EclipseSonar.getInstance(resource.getProject()).search(resourceKey).getCode();
      CompareUI.openCompareEditor(new SonarCompareInput(resource, source));
    }
  }

}
