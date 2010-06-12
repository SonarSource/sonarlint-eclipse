package org.sonar.ide.eclipse.views;

import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.sonar.wsclient.services.Measure;

/**
 * @author Evgeny Mandrikov
 */
public class MeasuresContentProvider implements IStructuredContentProvider {

  public void dispose() {
    // TODO Auto-generated method stub
  }

  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    // TODO Auto-generated method stub
  }

  public Object[] getElements(Object inputElement) {
    @SuppressWarnings("unchecked")
    final List<Measure> metrics = (List<Measure>) inputElement;
    return metrics.toArray();
  }

}
