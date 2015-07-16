/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.ide.eclipse.ui.internal.views.issues;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.views.markers.MarkerField;
import org.eclipse.ui.views.markers.MarkerItem;
import org.sonar.ide.eclipse.core.internal.markers.MarkerUtils;
import org.sonar.ide.eclipse.ui.internal.SonarImages;

/**
 * Each rule in Sonar has severity, so it seems logical to combine rule name and severity in one field.
 */
public class IssueDescriptionField extends MarkerField {

  /**
   * Create a new instance of the receiver.
   */
  public IssueDescriptionField() {
    super();
  }

  @Override
  public int getDefaultColumnWidth(Control control) {
    return 50 * getFontWidth(control);
  }

  public static final int getFontWidth(Control control) {
    GC gc = new GC(control.getDisplay());
    int width = gc.getFontMetrics().getAverageCharWidth();
    gc.dispose();
    return width;
  }

  @Override
  public String getValue(MarkerItem item) {
    return item.getAttributeValue(IMarker.MESSAGE, "Unknow");
  }

  @Override
  public int compare(MarkerItem item1, MarkerItem item2) {
    int severity1 = getSeverity(item1);
    int severity2 = getSeverity(item2);
    if (severity1 == severity2) {
      return super.compare(item1, item2);
    }
    return severity2 - severity1;
  }

  private static int getSeverity(MarkerItem item) {
    return convertSeverity(item.getAttributeValue(MarkerUtils.SONAR_MARKER_ISSUE_SEVERITY_ATTR, ""));
  }

  public static int convertSeverity(String severity) {
    String severityLower = severity != null ? severity.toLowerCase() : "";
    final int result;
    if (severityLower.startsWith("blocker")) {
      result = 0;
    } else if (severityLower.startsWith("critical")) {
      result = 1;
    } else if (severityLower.startsWith("major")) {
      result = 2;
    } else if (severityLower.startsWith("minor")) {
      result = 3;
    } else if (severityLower.startsWith("info")) {
      result = 4;
    } else {
      result = -1;
    }
    return result;
  }

  private static Image getImage(MarkerItem item) {
    if (item.getMarker() != null) {
      return getSeverityImage(getSeverity(item));
    } else {
      // If there is no marker maybe we have a groupBy item
      // First try with groupBy is new issue
      String msg = item.getAttributeValue(IMarker.MESSAGE, "");
      if (msg.startsWith("New issues")) {
        return SonarImages.IMG_NEW_ISSUE;
      } else if (msg.startsWith("Other issues")) {
        return SonarImages.IMG_ISSUE;
      }
      // GroupBy severity
      return getSeverityImage(convertSeverity(item.getAttributeValue(IMarker.MESSAGE, "")));
    }
  }

  private static Image getSeverityImage(int severity) {
    final Image result;
    switch (severity) {
      case -1:
        result = null;
        break;
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
    if (item != null) {
      cell.setImage(getImage(item));
    }
  }

}
