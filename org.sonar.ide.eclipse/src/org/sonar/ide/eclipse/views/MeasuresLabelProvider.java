package org.sonar.ide.eclipse.views;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.sonar.ide.shared.measures.MeasureData;

/**
 * @author Evgeny Mandrikov
 */
public class MeasuresLabelProvider extends LabelProvider implements ITableLabelProvider {

  protected static String[] COLUMNS = { "Domain", "Name", "Value" };

  public Image getColumnImage(final Object element, final int columnIndex) {
    return null;
  }

  public String getColumnText(final Object element, final int columnIndex) {
    final MeasureData measure = (MeasureData) element;
    switch (columnIndex) {
      case 0:
        return measure.getDomain();
      case 1:
        return measure.getName();
      case 2:
        return measure.getValue();
      default:
        throw new RuntimeException("Should not happen");
    }
  }
}
