package org.sonar.ide.eclipse.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.sonar.ide.eclipse.jobs.RefreshViolationJob;

/**
 * @author Jérémie Lagarde
 * 
 */
public class RefreshViolationAction implements IObjectActionDelegate {

  private IStructuredSelection selection;

  public RefreshViolationAction() {
    super();
  }

  /**
   * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
   */
  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
  }

  /**
   * @see IActionDelegate#run(IAction)
   */
  public void run(IAction action) {
    IProject[] projectList = null;
    Job job = new RefreshViolationJob((IProject) selection.getFirstElement());
    job.schedule();
  }

  /**
   * @see IActionDelegate#selectionChanged(IAction, ISelection)
   */
  public void selectionChanged(IAction action, ISelection selection) {
    if (selection instanceof IStructuredSelection) {
      this.selection = (IStructuredSelection) selection;
    }
  }

}
