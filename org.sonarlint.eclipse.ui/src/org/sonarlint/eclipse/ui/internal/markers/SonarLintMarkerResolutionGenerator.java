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

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.adapter.Adapters;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;

public class SonarLintMarkerResolutionGenerator implements IMarkerResolutionGenerator2 {

  @Override
  public boolean hasResolutions(final IMarker marker) {
    return isSonarLintIssueMarker(marker);
  }

  @Override
  public IMarkerResolution[] getResolutions(final IMarker marker) {
    List<IMarkerResolution> resolutions = new ArrayList<>();
    resolutions.add(new ShowRuleDescriptionMarkerResolver(marker));

    if (hasExtraLocations(marker)) {
      resolutions.add(new ShowHideIssueFlowsMarkerResolver(marker));
    }

    if (isStandaloneIssue(marker)) {
      resolutions.add(new DeactivateRuleMarkerResolver(marker));
    }

    // note: the display order seems independent from the order in this array
    return resolutions.toArray(new IMarkerResolution[resolutions.size()]);
  }

  private static boolean isSonarLintIssueMarker(IMarker marker) {
    try {
      return SonarLintCorePlugin.MARKER_ON_THE_FLY_ID.equals(marker.getType()) || SonarLintCorePlugin.MARKER_REPORT_ID.equals(marker.getType());
    } catch (final CoreException e) {
      return false;
    }
  }

  private static boolean hasExtraLocations(IMarker marker) {
    return !MarkerUtils.getIssueFlows(marker).isEmpty();
  }

  private static boolean isStandaloneIssue(IMarker marker) {
    ISonarLintFile sonarLintFile = Adapters.adapt(marker.getResource(), ISonarLintFile.class);
    if (sonarLintFile == null) {
      return false;
    }

    return !SonarLintCorePlugin.loadConfig(sonarLintFile.getProject()).isBound();
  }

}
