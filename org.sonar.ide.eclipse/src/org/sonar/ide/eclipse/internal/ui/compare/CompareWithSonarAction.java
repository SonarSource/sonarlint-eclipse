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

package org.sonar.ide.eclipse.internal.ui.compare;

import org.eclipse.compare.CompareUI;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.sonar.ide.api.SourceCode;
import org.sonar.ide.eclipse.core.ISonarResource;
import org.sonar.ide.eclipse.internal.EclipseSonar;
import org.sonar.ide.eclipse.utils.SelectionUtils;

/**
 * @author Jérémie Lagarde
 */
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
      CompareUI.openCompareEditor(new SonarCompareInput(resource.getResource(), sourceCode.getRemoteContent()));
    }
  }

}
