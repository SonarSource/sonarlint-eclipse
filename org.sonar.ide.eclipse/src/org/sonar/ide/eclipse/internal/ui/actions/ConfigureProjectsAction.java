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

package org.sonar.ide.eclipse.internal.ui.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.sonar.ide.eclipse.internal.ui.wizards.ConfigureProjectsWizard;
import org.sonar.ide.eclipse.ui.SonarUiPlugin;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * Inspired by org.eclipse.pde.internal.ui.wizards.tools.ConvertProjectsAction
 * 
 * @see ConfigureProjectsWizard
 */
public class ConfigureProjectsAction implements IObjectActionDelegate {

  private ISelection selection;

  public void run(IAction action) {
    List<IProject> unconfigured = getUnconfiguredProjects();
    if (unconfigured.isEmpty()) {
      // TODO show message
    }

    List<IProject> initialSelection = Lists.newArrayList();

    @SuppressWarnings("rawtypes")
    List elems = ((IStructuredSelection) selection).toList();
    for (Object elem : elems) {
      if (elem instanceof IProject) {
        initialSelection.add((IProject) elem);
      }
    }

    ConfigureProjectsWizard wizard = new ConfigureProjectsWizard(unconfigured, initialSelection);

    final Display display = getDisplay();
    final WizardDialog dialog = new WizardDialog(display.getActiveShell(), wizard);
    BusyIndicator.showWhile(display, new Runnable() {
      public void run() {
        dialog.open();
      }
    });
  }

  /**
   * @return open projects from workspace with Java nature and without Sonar nature
   */
  private List<IProject> getUnconfiguredProjects() {
    ArrayList<IProject> unconfigured = Lists.newArrayList();
    for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      if (project.isOpen() && !SonarUiPlugin.hasSonarNature(project) && SonarUiPlugin.hasJavaNature(project)) {
        unconfigured.add(project);
      }
    }
    return unconfigured;
  }

  public Display getDisplay() {
    Display display = Display.getCurrent();
    if (display == null) {
      display = Display.getDefault();
    }
    return display;
  }

  public void selectionChanged(IAction action, ISelection selection) {
    this.selection = selection;
  }

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
  }

}
