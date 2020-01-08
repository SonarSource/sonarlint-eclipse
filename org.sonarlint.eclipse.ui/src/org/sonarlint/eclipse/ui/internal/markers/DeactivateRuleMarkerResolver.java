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
import org.sonarlint.eclipse.ui.internal.util.DeactivateRuleUtils;

public class DeactivateRuleMarkerResolver implements IMarkerResolution2 {

  private static final String ZERO_WIDTH_SPACE = "\u200b";

  private final IMarker marker;

  public DeactivateRuleMarkerResolver(IMarker marker) {
    this.marker = marker;
  }

  @Override
  public String getDescription() {
    return "Deactivate rule in standalone analysis: " + marker.getAttribute(IMarker.MESSAGE, "unknown");
  }

  @Override
  public String getLabel() {
    // Note: quick fixes are ordered by label.
    // This zero-width space hack makes the quick fix displayed after other, more important quick fixes
    return ZERO_WIDTH_SPACE + "Deactivate rule " + marker.getAttribute(MarkerUtils.SONAR_MARKER_RULE_KEY_ATTR, "unknown");
  }

  @Override
  public void run(IMarker marker) {
    DeactivateRuleUtils.deactivateRule(marker);
  }

  @Override
  public Image getImage() {
    return SonarLintImages.RESOLUTION_DISABLE_RULE;
  }
}
