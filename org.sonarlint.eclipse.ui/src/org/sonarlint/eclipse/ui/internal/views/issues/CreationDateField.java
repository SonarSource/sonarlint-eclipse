/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.ui.internal.views.issues;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.views.markers.MarkerField;
import org.eclipse.ui.views.markers.MarkerItem;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarsource.sonarlint.core.client.api.util.DateUtils;

public class CreationDateField extends MarkerField {

  @Nullable
  @Override
  public String getValue(@Nullable MarkerItem item) {
    if (item == null) {
      return null;
    }
    String time = item.getAttributeValue(MarkerUtils.SONAR_MARKER_CREATION_DATE_ATTR, null);
    if (time == null) {
      // Persistent markers before 1.2 don't have creation date attribute
      return null;
    }
    return DateUtils.toAge(Long.valueOf(time));
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
    var gc = new GC(control.getDisplay());
    var width = gc.getFontMetrics().getAverageCharWidth();
    gc.dispose();
    return width;
  }
}
