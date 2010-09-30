package org.sonar.ide.eclipse.views;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import java.util.List;

public class ArrayContentProvider implements IStructuredContentProvider {
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
  }

  public void dispose() {
  }

  public Object[] getElements(Object inputElement) {
    if (inputElement.getClass().isArray()) {
      return (Object[]) inputElement;
    }
    if (inputElement instanceof List) {
      return ((List) inputElement).toArray();
    }
    return null;
  }
}
