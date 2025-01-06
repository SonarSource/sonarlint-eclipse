/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.markers;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.eclipse.core.runtime.CoreException;
import org.sonarlint.eclipse.core.SonarLintLogger;

public class MarkerFlows {

  private final List<MarkerFlow> flows;

  public MarkerFlows(List<MarkerFlow> flows) {
    this.flows = flows;
  }

  public List<MarkerFlow> getFlows() {
    return flows;
  }

  public void deleteAllMarkers() {
    flows.stream()
      .flatMap(f -> f.getLocations().stream())
      .map(MarkerFlowLocation::getMarker)
      .filter(Objects::nonNull)
      .forEach(m -> {
        try {
          m.delete();
        } catch (CoreException e) {
          SonarLintLogger.get().error(e.getMessage(), e);
        }
      });
  }

  /**
   * Special case when all flows have a single location, this is called "secondary locations"
   */
  public boolean isSecondaryLocations() {
    return !flows.isEmpty() && flows.stream().allMatch(f -> f.getLocations().size() == 1);
  }

  public boolean isEmpty() {
    return flows.isEmpty();
  }

  public Stream<MarkerFlowLocation> allLocationsAsStream() {
    return flows.stream().flatMap(f -> f.getLocations().stream());
  }

  public int count() {
    return flows.size();
  }

  public String getSummaryDescription() {
    if (!isEmpty()) {
      String kind;
      int count;
      if (isSecondaryLocations() || count() == 1) {
        kind = "location";
        count = (int) flows.stream().flatMap(flow -> flow.locations.stream()).count();
      } else {
        kind = "flow";
        count = count();
      }
      return " [+" + count + " " + pluralize(kind, count) + "]";
    }
    return "";
  }

  private static String pluralize(String str, int count) {
    return count == 1 ? str : (str + "s");
  }
}
