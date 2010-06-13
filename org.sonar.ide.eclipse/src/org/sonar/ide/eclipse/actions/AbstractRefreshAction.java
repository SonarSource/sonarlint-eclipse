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
    createJob(resources).schedule();
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
}
