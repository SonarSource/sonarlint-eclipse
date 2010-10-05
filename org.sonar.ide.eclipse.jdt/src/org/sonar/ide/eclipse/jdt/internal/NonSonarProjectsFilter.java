package org.sonar.ide.eclipse.jdt.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.sonar.ide.eclipse.SonarPlugin;

/**
 * Filters non-Sonar projects
 */
public class NonSonarProjectsFilter extends ViewerFilter {

  @Override
  public boolean select(Viewer viewer, Object parentElement, Object element) {
    if (element instanceof IJavaProject) {
      // For Package Explorer
      return SonarPlugin.hasSonarNature(((IJavaProject) element).getProject());
    } else if (element instanceof IProject) {
      // For Project Explorer
      return SonarPlugin.hasSonarNature((IProject) element);
    }
    return true;
  }
}
