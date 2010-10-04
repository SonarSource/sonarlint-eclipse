package org.sonar.ide.eclipse.actions;

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
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.wizards.ConfigureProjectsWizard;

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

  private List<IProject> getUnconfiguredProjects() {
    ArrayList<IProject> unconfigured = Lists.newArrayList();
    for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      if (project.isOpen() && !SonarPlugin.hasSonarNature(project)) {
        unconfigured.add(project);
      }
    }
    return unconfigured;
  }

  public Display getDisplay() {
    Display display = Display.getCurrent();
    if (display == null)
      display = Display.getDefault();
    return display;
  }

  public void selectionChanged(IAction action, ISelection selection) {
    this.selection = selection;
  }

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
  }

}
