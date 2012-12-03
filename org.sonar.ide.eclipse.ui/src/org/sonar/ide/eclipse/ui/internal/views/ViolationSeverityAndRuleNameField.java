/*
 * Sonar Eclipse
 * Copyright (C) 2010-2012 SonarSource
 * dev@sonar.codehaus.org
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
package org.sonar.ide.eclipse.ui.internal.views;

import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.markers.MarkerField;
import org.eclipse.ui.views.markers.MarkerItem;
import org.sonar.ide.eclipse.ui.internal.SonarImages;

/**
 * Each rule in Sonar has severity, so it seems logical to combine rule name and severity in one field.
 */
public class ViolationSeverityAndRuleNameField extends MarkerField {

  @Override
  public int compare(MarkerItem item1, MarkerItem item2) {
    int severity1 = getSeverity(item1);
    int severity2 = getSeverity(item2);
    if (severity1 == severity2) {
      super.compare(item1, item2);
    }
    return severity2 - severity1;
  }

  private int getSeverity(MarkerItem item) {
    return convertSeverity(item.getMarker().getAttribute("rulepriority", ""));
  }

  public static int convertSeverity(String severity) {
    if ("blocker".equalsIgnoreCase(severity)) {
      return 0;
    }
    if ("critical".equalsIgnoreCase(severity)) {
      return 1;
    }
    if ("major".equalsIgnoreCase(severity)) {
      return 2;
    }
    if ("minor".equalsIgnoreCase(severity)) {
      return 3;
    }
    if ("info".equalsIgnoreCase(severity)) {
      return 4;
    }
    return 4;
  }

  @Override
  public String getValue(MarkerItem item) {
    if ((item == null) || (item.getMarker() == null)) {
      return null;
    }
    return item.getMarker().getAttribute("rulename", "");
  }

  private Image getImage(MarkerItem item) {
    return getSeverityImage(getSeverity(item));
  }

  private Image getSeverityImage(int severity) {
    switch (severity) {
      case 0:
        return SonarImages.IMG_SEVERITY_BLOCKER;
      case 1:
        return SonarImages.IMG_SEVERITY_CRITICAL;
      case 2:
        return SonarImages.IMG_SEVERITY_MAJOR;
      case 3:
        return SonarImages.IMG_SEVERITY_MINOR;
      case 4:
        return SonarImages.IMG_SEVERITY_INFO;
      default:
        throw new IllegalArgumentException();
    }
  }

  /**
   * TODO see {@link #annotateImage(MarkerItem, Image)}
   */
  @Override
  public void update(ViewerCell cell) {
    super.update(cell);

    MarkerItem item = (MarkerItem) cell.getElement();
    cell.setImage(getImage(item));
  }

}
