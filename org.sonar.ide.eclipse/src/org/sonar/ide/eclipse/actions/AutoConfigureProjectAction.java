package org.sonar.ide.eclipse.actions;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.IWorkingSet;
import org.sonar.ide.eclipse.jobs.AutoConfigureProjectJob;

/**
 * @author Jérémie Lagarde
 */
public class AutoConfigureProjectAction implements IWorkbenchWindowActionDelegate {

  private IStructuredSelection selection;

  public AutoConfigureProjectAction() {
    super();
  }

  public void dispose() {
  }

  public void init(IWorkbenchWindow window) {
  }

  public void run(IAction action) {
    Job job = new AutoConfigureProjectJob(getProjects());
    job.schedule();
  }

  public void selectionChanged(IAction action, ISelection selection) {
    if (selection instanceof IStructuredSelection) {
      this.selection = (IStructuredSelection) selection;
    } else {
      this.selection = null;
    }
  }

  private IProject[] getProjects() {
    ArrayList<IProject> projectList = new ArrayList<IProject>();
    if (selection != null) {
      for (Iterator<?> it = selection.iterator(); it.hasNext();) {
        Object o = it.next();
        if (o instanceof IProject) {
          projectList.add((IProject) o);
        } else if (o instanceof IWorkingSet) {
          IWorkingSet workingSet = (IWorkingSet) o;
          for (IAdaptable adaptable : workingSet.getElements()) {
            IProject project = (IProject) adaptable.getAdapter(IProject.class);
            if (project != null && project.isAccessible()) {
              projectList.add(project);
            }
          }
        }
      }
    }
    if (projectList.isEmpty()) {
      return ResourcesPlugin.getWorkspace().getRoot().getProjects();
    }
    return projectList.toArray(new IProject[projectList.size()]);
  }

}
