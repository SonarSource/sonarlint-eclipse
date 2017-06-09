/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2017 SonarSource SA
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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;

public class SonarLintMarkerResolutionGenerator implements IMarkerResolutionGenerator2 {

  @Override
  public boolean hasResolutions(final IMarker marker) {
    try {
      return SonarLintCorePlugin.MARKER_ON_THE_FLY_ID.equals(marker.getType()) && marker.getAttribute(MarkerUtils.SONAR_MARKER_HAS_EXTRA_LOCATION_KEY_ATTR, false);
    } catch (final CoreException e) {
      return false;
    }
  }

  @Override
  public IMarkerResolution[] getResolutions(final IMarker marker) {
    if (hasResolutions(marker)) {
      return new IMarkerResolution[] {new ShowIssueFlowsMarkerResolver(marker), new ShowRuleDescriptionMarkerResolver(marker)};
    } else {
      return new IMarkerResolution[] {new ShowRuleDescriptionMarkerResolver(marker)};
    }
  }

}
