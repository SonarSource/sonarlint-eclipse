/*
 * Sonar Eclipse
 * Copyright (C) 2010-2013 SonarSource
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
import org.sonar.ide.eclipse.core.internal.markers.MarkerUtils;
import org.sonar.ide.eclipse.ui.internal.SonarImages;

/**
 * Each rule in Sonar has severity, so it seems logical to combine rule name and severity in one field.
 */
public class IssueSeverityAndRuleNameField extends MarkerField {

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
    return convertSeverity(item.getMarker().getAttribute(MarkerUtils.SONAR_MARKER_RULE_PRIORITY_ATTR, ""));
  }

  public static int convertSeverity(String severity) {
    final int result;
    if ("blocker".equalsIgnoreCase(severity)) {
      result = 0;
    }
    else if ("critical".equalsIgnoreCase(severity)) {
      result = 1;
    }
    else if ("major".equalsIgnoreCase(severity)) {
      result = 2;
    }
    else if ("minor".equalsIgnoreCase(severity)) {
      result = 3;
    }
    else if ("info".equalsIgnoreCase(severity)) {
      result = 4;
    }
    else {
      result = 4;
    }
    return result;
  }

  @Override
  public String getValue(MarkerItem item) {
    if ((item == null) || (item.getMarker() == null)) {
      return null;
    }
    return item.getMarker().getAttribute(MarkerUtils.SONAR_MARKER_RULE_NAME_ATTR, "");
  }

  private Image getImage(MarkerItem item) {
    return getSeverityImage(getSeverity(item));
  }

  private Image getSeverityImage(int severity) {
    final Image result;
    switch (severity) {
      case 0:
        result = SonarImages.IMG_SEVERITY_BLOCKER;
        break;
      case 1:
        result = SonarImages.IMG_SEVERITY_CRITICAL;
        break;
      case 2:
        result = SonarImages.IMG_SEVERITY_MAJOR;
        break;
      case 3:
        result = SonarImages.IMG_SEVERITY_MINOR;
        break;
      case 4:
        result = SonarImages.IMG_SEVERITY_INFO;
        break;
      default:
        throw new IllegalArgumentException();
    }
    return result;
  }

  @Override
  public void update(ViewerCell cell) {
    super.update(cell);

    MarkerItem item = (MarkerItem) cell.getElement();
    cell.setImage(getImage(item));
  }

}
