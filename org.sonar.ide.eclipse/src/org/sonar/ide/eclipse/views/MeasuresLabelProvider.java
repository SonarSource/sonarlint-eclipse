/*
 * Copyright (C) 2010 Evgeny Mandrikov
 *
 * Sonar-IDE is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar-IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar-IDE; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

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
