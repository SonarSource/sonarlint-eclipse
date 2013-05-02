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
package org.sonar.ide.eclipse.ui.internal.compare;

import org.sonar.ide.eclipse.ui.internal.remote.SourceCode;

import org.sonar.ide.eclipse.ui.internal.remote.EclipseSonar;
import org.eclipse.compare.CompareUI;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.sonar.ide.eclipse.core.resources.ISonarResource;
import org.sonar.ide.eclipse.ui.internal.util.SelectionUtils;

public class CompareWithSonarAction implements IWorkbenchWindowActionDelegate {

  private ISonarResource resource;

  public void dispose() {
    resource = null;
  }

  public void init(final IWorkbenchWindow window) {
  }

  public void selectionChanged(final IAction action, final ISelection selection) {
    resource = null;
    Object element = SelectionUtils.getSingleElement(selection);
    if (element instanceof ISonarResource) {
      resource = (ISonarResource) element;
    }
  }

  public void run(final IAction action) {
    if (resource != null) {
      final SourceCode sourceCode = EclipseSonar.getInstance(resource.getProject()).search(resource);
      if (sourceCode != null) {
        CompareUI.openCompareEditor(new SonarCompareInput(resource.getResource(), sourceCode.getRemoteContent()));
      } else {
        MessageDialog.openInformation(
            PlatformUI.getWorkbench().getDisplay().getActiveShell(),
            "Not found",
            resource.getKey() + " not found on server");
      }
    }
  }
}
