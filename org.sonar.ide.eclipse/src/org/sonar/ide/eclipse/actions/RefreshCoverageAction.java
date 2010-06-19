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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.jobs.RefreshCoverageJob;

/**
 * @author Jérémie Lagarde
 */
public class RefreshCoverageAction implements IEditorActionDelegate {

  protected AbstractDecoratedTextEditor targetEditor;
  protected IStructuredSelection        selection;

  public void setActiveEditor(final IAction action, final IEditorPart targetEditor) {
    this.targetEditor = null;
    if (targetEditor instanceof AbstractDecoratedTextEditor) {
      this.targetEditor = (AbstractDecoratedTextEditor) targetEditor;
      selection = null;
    }
  }

  public void run(final IAction action) {
    if (targetEditor != null) {
      refresh(targetEditor);
      return;
    }
    if (selection != null) {
      final IWorkbenchPage page = SonarPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage();
      for (final Object element : selection.toList()) {
        if (element instanceof IFile) {
          final IFile file = (IFile) element;
          final IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(file.getName());
          try {
            refresh((AbstractDecoratedTextEditor) page.openEditor(new FileEditorInput(file), desc.getId()));
          } catch (final PartInitException e) {
            SonarPlugin.getDefault().displayError(IStatus.WARNING, "Error in RefreshCoverageAction.", e, true); //$NON-NLS-1$
          }
        }
      }
    }
  }

  private void refresh(final AbstractDecoratedTextEditor targetEditor) {
    final Job job = new RefreshCoverageJob(targetEditor);
    job.schedule();
  }

  public void selectionChanged(final IAction action, final ISelection selection) {
    this.selection = null;
    this.targetEditor = null;
    if (selection instanceof IStructuredSelection) {
      this.selection = (IStructuredSelection) selection;
    }
  }

}