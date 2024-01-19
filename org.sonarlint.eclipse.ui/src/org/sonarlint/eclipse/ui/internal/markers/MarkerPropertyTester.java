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
package org.sonarlint.eclipse.ui.internal.markers;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.views.markers.MarkerItem;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;

public class MarkerPropertyTester extends PropertyTester {
  private static final String HAS_QUICK_FIX = "hasQuickFix"; //$NON-NLS-1$
  private static final String CAN_BE_RESOLVED = "canBeResolved"; //$NON-NLS-1$
  private static final String RESOLVED_ISSUE = "resolvedIssue"; //$NON-NLS-1$

  /**
   * Create a new instance of the receiver.
   */
  public MarkerPropertyTester() {
    super();
  }

  @Override
  public boolean test(Object receiver, String property, Object[] args,
    Object expectedValue) {
    var markerItem = (MarkerItem) receiver;
    var marker = markerItem.getMarker();
    // SLE-482 marker can be null for category rows when grouping by severity for example
    if (marker != null) {
      switch (property) {
        case HAS_QUICK_FIX:
          return !isResolved(marker) && !MarkerUtils.getIssueQuickFixes(marker).getQuickFixes().isEmpty();
        case CAN_BE_RESOLVED:
          return !isResolved(marker) && canBeResolved(marker);
        case RESOLVED_ISSUE:
          return isResolved(marker);
      }
    }
    return false;
  }
  
  private static boolean isResolved(IMarker marker) {
    return marker.getAttribute(MarkerUtils.SONAR_MARKER_RESOLVED_ATTR, false);
  }
  
  private static boolean canBeResolved(IMarker marker) {
    return marker.getAttribute(MarkerUtils.SONAR_MARKER_SERVER_ISSUE_KEY_ATTR, null) != null
      || marker.getAttribute(MarkerUtils.SONAR_MARKER_ANTICIPATED_ISSUE_ATTR, false);
  }
}
