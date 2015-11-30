/*
 * SonarLint for Eclipse
 * Copyright (C) 2015 SonarSource
 * sonarlint@sonarsource.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonarlint.eclipse.ui.internal.views.issues;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.views.markers.MarkerField;
import org.eclipse.ui.views.markers.MarkerItem;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;

public class CreationDateField extends MarkerField {

  private final SimpleDateFormat sdf = new SimpleDateFormat();

  @Override
  public String getValue(MarkerItem item) {
    if (item == null) {
      return null;
    }
    String time = item.getAttributeValue(MarkerUtils.SONAR_MARKER_CREATION_DATE_ATTR, null);
    return time != null ? sdf.format(new Date(Long.valueOf(time))) : null;
  }

  @Override
  public int compare(MarkerItem item1, MarkerItem item2) {
    // Compare in reverse order to make newest issues first by default
    return Long.valueOf(item2.getAttributeValue(MarkerUtils.SONAR_MARKER_CREATION_DATE_ATTR, "0"))
      .compareTo(Long.valueOf(item1.getAttributeValue(MarkerUtils.SONAR_MARKER_CREATION_DATE_ATTR, "0")));
  }

  @Override
  public int getDefaultColumnWidth(Control control) {
    return 30 * getFontWidth(control);
  }

  public static final int getFontWidth(Control control) {
    GC gc = new GC(control.getDisplay());
    int width = gc.getFontMetrics().getAverageCharWidth();
    gc.dispose();
    return width;
  }
}
