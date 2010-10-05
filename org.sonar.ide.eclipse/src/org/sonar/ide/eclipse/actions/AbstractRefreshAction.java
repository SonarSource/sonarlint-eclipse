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
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * @author Jérémie Lagarde
 */
public abstract class AbstractRefreshAction implements IWorkbenchWindowActionDelegate {

  private IStructuredSelection selection;

  public AbstractRefreshAction() {
    super();
  }

  public void dispose() {
  }

  public void init(final IWorkbenchWindow window) {
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
    Job[] jobs = createJobs(resources);
    for (Job job : jobs) {
      job.schedule();
    }
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

  protected abstract Job createJob(List<IResource> resources);

  protected Job[] createJobs(List<IResource> resources) {
    return new Job[] { createJob(resources) };
  }
}
