package org.sonar.ide.eclipse.views;

import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * @author Evgeny Mandrikov
 */
public class MapContentProvider implements ITreeContentProvider {

  @SuppressWarnings("unchecked")
  public Object[] getChildren(Object parentElement) {
    if (parentElement instanceof Map) {
      return ((Map) parentElement).entrySet().toArray();
    }
    if (parentElement instanceof Map.Entry) {
      Map.Entry entry = (Map.Entry) parentElement;
      Object value = entry.getValue();
      if (value instanceof List) {
        return ((List) value).toArray();
      }
      return new Object[] { value };
    }
    return new Object[0];
  }

  public Object getParent(Object element) {
    return null;
  }

  public boolean hasChildren(Object element) {
    return getChildren(element).length > 0;
  }

  public Object[] getElements(Object inputElement) {
    return getChildren(inputElement);
  }

  public void dispose() {
  }

  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
  }

}
