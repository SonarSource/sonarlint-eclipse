package org.sonar.ide.eclipse.ui;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;

public abstract class AbstractTableLabelProvider implements ITableLabelProvider {

  /**
   * {@inheritDoc}
   */
  public Image getColumnImage(Object element, int columnIndex) {
    return null;
  }

  public abstract String getColumnText(Object element, int columnIndex);

  /**
   * {@inheritDoc}
   */
  public void addListener(ILabelProviderListener listener) {
  }

  /**
   * {@inheritDoc}
   */
  public void dispose() {
  }

  /**
   * {@inheritDoc}
   */
  public boolean isLabelProperty(Object element, String property) {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public void removeListener(ILabelProviderListener listener) {
  }

}
