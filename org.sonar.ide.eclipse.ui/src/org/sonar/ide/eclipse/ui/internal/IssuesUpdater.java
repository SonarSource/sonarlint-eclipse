/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
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
package org.sonar.ide.eclipse.ui.internal;

import org.eclipse.core.resources.IResource;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.sonar.ide.eclipse.core.internal.jobs.SynchronizeIssuesJob;

import java.util.Collections;

public class IssuesUpdater implements IPartListener2 {
  public void partOpened(IWorkbenchPartReference partRef) {
    IWorkbenchPart part = partRef.getPart(true);
    if (part instanceof IEditorPart) {
      IEditorInput input = ((IEditorPart) part).getEditorInput();
      if (input instanceof IFileEditorInput) {
        IResource resource = ((IFileEditorInput) input).getFile();
        new SynchronizeIssuesJob(Collections.singletonList(resource), false).schedule();
      }
    }
  }

  public void partVisible(IWorkbenchPartReference partRef) {
    // Nothing to do
  }

  public void partInputChanged(IWorkbenchPartReference partRef) {
    // Nothing to do
  }

  public void partHidden(IWorkbenchPartReference partRef) {
    // Nothing to do
  }

  public void partDeactivated(IWorkbenchPartReference partRef) {
    // Nothing to do
  }

  public void partClosed(IWorkbenchPartReference partRef) {
    // Nothing to do
  }

  public void partBroughtToTop(IWorkbenchPartReference partRef) {
    // Nothing to do
  }

  public void partActivated(IWorkbenchPartReference partRef) {
    // Nothing to do
  }
}
