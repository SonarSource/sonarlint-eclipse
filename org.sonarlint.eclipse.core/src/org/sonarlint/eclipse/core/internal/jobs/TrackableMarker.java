/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.jobs;

import org.eclipse.core.resources.IMarker;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.tracking.Trackable;

class TrackableMarker implements Trackable {

  private final IMarker marker;

  public TrackableMarker(IMarker marker) {
    this.marker = marker;
  }

  public IMarker getWrapped() {
    return marker;
  }

  @Override
  public Integer getLine() {
    int line = marker.getAttribute(IMarker.LINE_NUMBER, 0);
    return line != 0 ? line : null;
  }

  @Override
  public String getMessage() {
    return marker.getAttribute(IMarker.MESSAGE, "");
  }

  @Override
  public Integer getTextRangeHash() {
    int attribute = marker.getAttribute(MarkerUtils.SONAR_MARKER_CHECKSUM_ATTR, 0);
    return attribute != 0 ? attribute : null;
  }

  @Override
  public Integer getLineHash() {
    // TODO
    return null;
  }

  @Override
  public String getRuleKey() {
    return marker.getAttribute(MarkerUtils.SONAR_MARKER_RULE_KEY_ATTR, "");
  }

  public Long getCreationDate() {
    String attribute = marker.getAttribute(MarkerUtils.SONAR_MARKER_CREATION_DATE_ATTR, null);
    return attribute != null ? Long.parseLong(attribute) : null;
  }

  public String getServerIssueKey() {
    return marker.getAttribute(MarkerUtils.SONAR_MARKER_SERVER_ISSUE_KEY_ATTR, null);
  }

  public boolean isResolved() {
    return marker.getAttribute(MarkerUtils.SONAR_MARKER_RESOLVED_ATTR, false);
  }

  public String getAssignee() {
    return marker.getAttribute(MarkerUtils.SONAR_MARKER_ASSIGNEE_ATTR, "");
  }

}
