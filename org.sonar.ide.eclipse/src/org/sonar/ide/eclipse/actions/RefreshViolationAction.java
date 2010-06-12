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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.sonar.ide.eclipse.jobs.RefreshCoverageJob;
import org.sonar.ide.eclipse.jobs.RefreshDuplicationsJob;
import org.sonar.ide.eclipse.jobs.RefreshViolationJob;

/**
 * @author Jérémie Lagarde
 */
public class RefreshViolationAction implements IWorkbenchWindowActionDelegate {

  private IStructuredSelection selection;

  public RefreshViolationAction() {
    super();
  }

  public void dispose() {
  }

  public void init(final IWorkbenchWindow window) {
  }

  /**
   * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
   */
  public void setActivePart(final IAction action, final IWorkbenchPart targetPart) {
  }

  /**
   * @see IActionDelegate#run(IAction)
   */
  public void run(final IAction action) {
    final List<IResource> resources;
    if (selection instanceof ITreeSelection) {
      resources = new ArrayList<IResource>();
      Collections.addAll(resources, ResourcesPlugin.getWorkspace().getRoot().getProjects());
    } else {
      resources = selection.toList();
    }
    // Load violations
    final Job violationsJob = new RefreshViolationJob(resources);
    violationsJob.schedule();
    // Load duplications
    final Job duplicationsJob = new RefreshDuplicationsJob(resources);
    duplicationsJob.schedule();
    // Load coverage
    final Job coverageJob = new RefreshCoverageJob(resources);
    coverageJob.schedule();
  }

  /**
   * @see IActionDelegate#selectionChanged(IAction, ISelection)
   */
  public void selectionChanged(final IAction action, final ISelection selection) {
    if (selection instanceof IStructuredSelection) {
      this.selection = (IStructuredSelection) selection;
    } else {
      this.selection = null;
    }
  }
}
