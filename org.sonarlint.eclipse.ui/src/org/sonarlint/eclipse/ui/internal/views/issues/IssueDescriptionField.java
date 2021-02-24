/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2021 SonarSource SA
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

import java.util.List;
import java.util.Locale;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.views.markers.MarkerField;
import org.eclipse.ui.views.markers.MarkerItem;
import org.sonarlint.eclipse.core.internal.markers.MarkerFlow;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.utils.CompatibilityUtils;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;

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
    return 100 * getFontWidth(control);
  }

  public static final int getFontWidth(Control control) {
    GC gc = new GC(control.getDisplay());
    int width = gc.getFontMetrics().getAverageCharWidth();
    gc.dispose();
    return width;
  }

  @Override
  public String getValue(MarkerItem item) {
    StringBuilder sb = new StringBuilder();
    sb.append(item.getAttributeValue(IMarker.MESSAGE, "No message"));
    IMarker marker = item.getMarker();
    // When grouping by severity, MarkerItem will be a MarkerCategory, that doesn't have an attached marker
    if (marker != null) {
      List<MarkerFlow> issueFlows = MarkerUtils.getIssueFlows(marker);
      if (!issueFlows.isEmpty()) {
        boolean isSecondary = MarkerUtils.isSecondaryLocations(issueFlows);
        String kind;
        if (isSecondary) {
          kind = "location";
        } else {
          kind = "flow";
        }
        sb.append(" [+").append(issueFlows.size()).append(" ").append(pluralize(kind, issueFlows.size())).append("]");
      }
    }
    return sb.toString();
  }

  private static String pluralize(String str, int count) {
    return count == 1 ? str : (str + "s");
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

  public static int convertSeverity(@Nullable String severity) {
    String severityLower = severity != null ? severity.toLowerCase(Locale.ENGLISH) : "";
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

  @Nullable
  private static Image getImage(MarkerItem item) {
    if (item.getMarker() != null) {
      if (CompatibilityUtils.supportRectangleImagesInTreeViewer()) {
        return SonarLintImages.getIssueImage(item.getAttributeValue(MarkerUtils.SONAR_MARKER_ISSUE_SEVERITY_ATTR, "major"),
          item.getAttributeValue(MarkerUtils.SONAR_MARKER_ISSUE_TYPE_ATTR, "code_smell"));
      } else {
        return SonarLintImages.getSeverityImage(item.getAttributeValue(MarkerUtils.SONAR_MARKER_ISSUE_SEVERITY_ATTR, "major"));
      }
    } else {
      // If there is no marker maybe we have a groupBy item
      // GroupBy severity
      String severity = item.getAttributeValue(IMarker.MESSAGE, "");
      if (severity.indexOf(' ') >= 0) {
        severity = severity.substring(0, severity.indexOf(' '));
      }
      if (CompatibilityUtils.supportRectangleImagesInTreeViewer()) {
        // All images of a TreeItem should have the same size
        return SonarLintImages.getIssueImage(severity, null);
      } else {
        return SonarLintImages.getSeverityImage(severity);
      }
    }
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
