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
package org.sonarlint.eclipse.ui.internal.markers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.adapter.Adapters;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.quickfixes.MarkerQuickFix;
import org.sonarlint.eclipse.core.internal.utils.CompatibilityUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.ui.internal.extension.SonarLintUiExtensionTracker;
import org.sonarlint.eclipse.ui.quickfixes.IMarkerResolutionEnhancer;
import org.sonarlint.eclipse.ui.quickfixes.ISonarLintMarkerResolver;

import static java.util.stream.Collectors.toList;

public class SonarLintMarkerResolutionGenerator implements IMarkerResolutionGenerator2 {

  // See org.eclipse.jdt.internal.ui.text.correction.IProposalRelevance
  private static final int RESOLUTION_RELEVANCE_LOWER_BOUND = -10;

  // See org.eclipse.jdt.internal.ui.text.correction.IProposalRelevance
  private static final int RESOLUTION_RELEVANCE_HIGHER_BOUND = 15;

  @Override
  public boolean hasResolutions(final IMarker marker) {
    return isSonarLintIssueMarker(marker);
  }

  @Override
  public IMarkerResolution[] getResolutions(final IMarker marker) {
    List<SortableMarkerResolver> resolutions = new ArrayList<>();

    // note: the display order is independent from the order in this list (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=232383)

    resolutions.addAll(getQuickFixesResolutions(marker));

    if (hasExtraLocations(marker)) {
      resolutions.add(new ShowHideIssueFlowsMarkerResolver(marker, RESOLUTION_RELEVANCE_LOWER_BOUND - 1));
    }

    resolutions.add(new ShowRuleDescriptionMarkerResolver(marker, RESOLUTION_RELEVANCE_LOWER_BOUND - 2));

    if (isStandaloneIssue(marker)) {
      resolutions.add(new DeactivateRuleMarkerResolver(marker, RESOLUTION_RELEVANCE_LOWER_BOUND - 3));
    }

    return resolutions.stream()
      .map(SonarLintMarkerResolutionGenerator::enhanceWithResolutionRelevance)
      .map(r -> enhance(r, marker))
      .collect(Collectors.toList())
      .toArray(new IMarkerResolution[resolutions.size()]);
  }

  private static List<SortableMarkerResolver> getQuickFixesResolutions(IMarker marker) {
    return MarkerUtils.getIssueQuickFixes(marker).getQuickFixes()
      .stream()
      .filter(MarkerQuickFix::isValid)
      .map(fix -> new ApplyQuickFixMarkerResolver(fix, RESOLUTION_RELEVANCE_HIGHER_BOUND + 1))
      .collect(toList());
  }

  private static ISonarLintMarkerResolver enhance(ISonarLintMarkerResolver target, IMarker marker) {
    ISonarLintMarkerResolver enhanced = target;
    for (IMarkerResolutionEnhancer markerResolutionEnhancer : SonarLintUiExtensionTracker.getInstance().getMarkerResolutionEnhancers()) {
      enhanced = markerResolutionEnhancer.enhance(enhanced, marker);
    }
    return enhanced;
  }

  private static ISonarLintMarkerResolver enhanceWithResolutionRelevance(ISonarLintMarkerResolver target) {
    if (CompatibilityUtils.supportMarkerResolutionRelevance()) {
      return new MarkerResolutionRelevanceAdapter(target);
    }
    return target;
  }

  private static boolean isSonarLintIssueMarker(IMarker marker) {
    try {
      return MarkerUtils.SONARLINT_PRIMARY_MARKER_IDS.contains(marker.getType());
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
