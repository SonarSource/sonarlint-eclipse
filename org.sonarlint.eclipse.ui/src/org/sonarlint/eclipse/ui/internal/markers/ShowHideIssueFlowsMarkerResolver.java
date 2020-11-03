/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2020 SonarSource SA
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

import org.eclipse.core.resources.IMarker;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution2;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;

public class ShowHideIssueFlowsMarkerResolver implements IMarkerResolution2 {

  private final IMarker marker;
  private final boolean alreadySelected;
  private final boolean isSecondaryLocation;

  public ShowHideIssueFlowsMarkerResolver(IMarker marker) {
    this.marker = marker;
    this.alreadySelected = marker.equals(SonarLintUiPlugin.getSonarlintMarkerSelectionService().getLastSelectedMarker().orElse(null));
    isSecondaryLocation = MarkerUtils.isSecondaryLocations(MarkerUtils.getIssueFlows(marker));
  }

  @Override
  public String getDescription() {
    String action;
    String theIssue = " the issue: " + marker.getAttribute(IMarker.MESSAGE, "unknown");
    String suffix;
    if (alreadySelected) {
      action = "Hide";
      suffix = " of" + theIssue;
    } else {
      action = "Show";
      suffix = " to better understand" + theIssue;
    }
    if (isSecondaryLocation) {
      return action + " all locations" + suffix;
    } else {
      return action + " data flows" + suffix;
    }
  }

  @Override
  public String getLabel() {
    String action;
    if (alreadySelected) {
      action = "Hide";
    } else {
      action = "Show";
    }
    if (isSecondaryLocation) {
      return action + " issue locations";
    } else {
      return action + " issue data flows";
    }
  }

  @Override
  public void run(IMarker marker) {
    SonarLintUiPlugin.getSonarlintMarkerSelectionService().markerSelected(alreadySelected ? null : marker, true, !alreadySelected);
  }

  @Override
  public Image getImage() {
    return SonarLintImages.RESOLUTION_SHOW_LOCATIONS;
  }
}
