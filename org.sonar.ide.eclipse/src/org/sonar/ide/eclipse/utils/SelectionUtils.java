package org.sonar.ide.eclipse.utils;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

public class SelectionUtils {

  /**
   * Returns the selected element if the selection consists of a single
   * element only.
   * 
   * @param s the selection
   * @return the selected first element or null
   */
  public static Object getSingleElement(ISelection s) {
    if ( !(s instanceof IStructuredSelection)) {
      return null;
    }
    IStructuredSelection selection = (IStructuredSelection) s;
    if (selection.size() != 1) {
      return null;
    }
    return selection.getFirstElement();
  }

  private SelectionUtils() {
  }

}
