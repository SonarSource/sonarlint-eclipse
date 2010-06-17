/*
 * Copyright (C) 2010 Evgeny Mandrikov, Jérémie Lagarde
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
package org.sonar.ide.eclipse.markers;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;

/**
 * @author Jérémie Lagarde
 */
public class ShowCoverageAction implements IEditorActionDelegate {

  protected AbstractDecoratedTextEditor targetEditor;

  public void setActiveEditor(final IAction action, final IEditorPart targetEditor) {
    this.targetEditor = null;
    if (targetEditor instanceof AbstractDecoratedTextEditor) {
      this.targetEditor = (AbstractDecoratedTextEditor) targetEditor;
    }
  }

  public void run(final IAction action) {
    final Job job = new ShowCoverageJob(targetEditor);
    job.schedule();
  }

  public void selectionChanged(final IAction action, final ISelection selection) {
  }

}
