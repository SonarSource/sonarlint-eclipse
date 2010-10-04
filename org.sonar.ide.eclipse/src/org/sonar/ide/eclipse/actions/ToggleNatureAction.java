package org.sonar.ide.eclipse.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.sonar.ide.eclipse.core.ISonarConstants;
import org.sonar.ide.eclipse.core.SonarLogger;

import com.google.common.collect.Lists;

import java.util.Iterator;
import java.util.List;

public class ToggleNatureAction implements IObjectActionDelegate {

  private ISelection selection;

  public void run(IAction action) {
    if (selection instanceof IStructuredSelection) {
      for (Iterator<?> it = ((IStructuredSelection) selection).iterator(); it.hasNext();) {
        Object element = it.next();
        IProject project = null;
        if (element instanceof IProject) {
          project = (IProject) element;
        } else if (element instanceof IAdaptable) {
          project = (IProject) ((IAdaptable) element).getAdapter(IProject.class);
        }
        if (project != null) {
          try {
            toggleNature(project);
          } catch (CoreException e) {
            SonarLogger.log(e);
          }
        }
      }
    }
  }

  private void toggleNature(IProject project) throws CoreException {
    if (project.hasNature(ISonarConstants.NATURE_ID)) {
      disableNature(project);
    } else {
      enableNature(project);
    }
  }

  public static void enableNature(IProject project) throws CoreException {
    IProjectDescription description = project.getDescription();
    String[] prevNatures = description.getNatureIds();
    String[] newNatures = new String[prevNatures.length + 1];
    System.arraycopy(prevNatures, 0, newNatures, 1, prevNatures.length);
    newNatures[0] = ISonarConstants.NATURE_ID;
    description.setNatureIds(newNatures);
    project.setDescription(description, null);
  }

  private void disableNature(IProject project) throws CoreException {
    project.deleteMarkers(ISonarConstants.MARKER_ID, true, IResource.DEPTH_INFINITE);

    IProjectDescription description = project.getDescription();
    List<String> newNatures = Lists.newArrayList();
    for (String natureId : description.getNatureIds()) {
      if ( !ISonarConstants.NATURE_ID.equals(natureId)) {
        newNatures.add(natureId);
      }
    }
    description.setNatureIds(newNatures.toArray(new String[newNatures.size()]));
    project.setDescription(description, null);
  }

  public void selectionChanged(IAction action, ISelection selection) {
    this.selection = selection;
  }

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
  }

}
