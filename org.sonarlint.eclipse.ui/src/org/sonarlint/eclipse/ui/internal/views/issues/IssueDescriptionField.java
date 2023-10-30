/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2023 SonarSource SA
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

import java.util.Locale;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.views.markers.MarkerField;
import org.eclipse.ui.views.markers.MarkerItem;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarsource.sonarlint.core.commons.ImpactSeverity;
import org.sonarsource.sonarlint.core.commons.IssueSeverity;

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
    var gc = new GC(control.getDisplay());
    var width = gc.getFontMetrics().getAverageCharWidth();
    gc.dispose();
    return width;
  }

  @Override
  public String getValue(MarkerItem item) {
    var sb = new StringBuilder();
    
    sb.append(item.getAttributeValue(IMarker.MESSAGE, "No message"));
    var marker = item.getMarker();
    // When grouping by severity, MarkerItem will be a MarkerCategory, that doesn't have an attached marker
    if (marker != null) {
      var issueFlows = MarkerUtils.getIssueFlows(marker);
      sb.append(issueFlows.getSummaryDescription());
    }
    return sb.toString();
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
    var severityLower = severity != null ? severity.toLowerCase(Locale.ENGLISH) : "";
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
      // Get the matching status of this markers' issue (found locally / on SC or SQ)
      var matchingStatus = MarkerUtils.getMatchingStatus(item.getMarker(),
        item.getAttributeValue(MarkerUtils.SONAR_MARKER_SERVER_ISSUE_KEY_ATTR, null));
      
      var isResolved = item.getAttributeValue(MarkerUtils.SONAR_MARKER_RESOLVED_ATTR, false);
      
      // We have to check if we want to display an old or new CCT issue
      var cleanCodeAttribute = MarkerUtils.decodeCleanCodeAttribute(
        item.getAttributeValue(MarkerUtils.SONAR_MARKER_ISSUE_ATTRIBUTE_ATTR, null));
      var highestImpact = MarkerUtils.decodeHighestImpact(
        item.getAttributeValue(MarkerUtils.SONAR_MARKER_ISSUE_HIGHEST_IMPACT_ATTR, null));
      if (cleanCodeAttribute == null || highestImpact == null) {
        return SonarLintImages.getIssueImage(matchingStatus,
          item.getAttributeValue(MarkerUtils.SONAR_MARKER_ISSUE_SEVERITY_ATTR, "major"),
          item.getAttributeValue(MarkerUtils.SONAR_MARKER_ISSUE_TYPE_ATTR, "code_smell"),
          isResolved);
      }
      
      return SonarLintImages.getIssueImage(matchingStatus, highestImpact.name(), isResolved);
    } else {
      // It is no actual marker but a groupBy item which groups the headers
      var groupByTitle = item.getAttributeValue(IMarker.MESSAGE, "")
        .replaceAll("\\(\\d+\\s+items?\\)", "")
        .toUpperCase(Locale.ENGLISH)
        .trim();
      
      // As we offer multiple different grouping options (severity / impacts), we have to display
      // the correct grouping icon!
      if (ImpactSeverity.HIGH.name().equals(groupByTitle)
        || ImpactSeverity.MEDIUM.name().equals(groupByTitle)
        || ImpactSeverity.LOW.name().equals(groupByTitle)) {
        return SonarLintImages.getIssueImage(null, groupByTitle, false);
      } else if (IssueSeverity.BLOCKER.name().equals(groupByTitle)
        || IssueSeverity.CRITICAL.name().equals(groupByTitle)
        || IssueSeverity.MAJOR.name().equals(groupByTitle)
        || IssueSeverity.MINOR.name().equals(groupByTitle)
        || IssueSeverity.INFO.name().equals(groupByTitle)) {
        return SonarLintImages.getIssueImage(null, groupByTitle, null, false);
      }
      return SonarLintImages.getNotAvailableImage();
    }
  }

  @Override
  public void update(ViewerCell cell) {
    super.update(cell);
    var item = (MarkerItem) cell.getElement();
    if (item != null) {
      cell.setImage(getImage(item));
    }
  }
}
