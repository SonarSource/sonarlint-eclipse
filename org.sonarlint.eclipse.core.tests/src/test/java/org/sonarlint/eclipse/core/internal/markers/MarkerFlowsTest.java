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
package org.sonarlint.eclipse.core.internal.markers;

import java.util.List;
import org.junit.Test;
import org.sonarlint.eclipse.tests.common.SonarTestCase;

import static org.assertj.core.api.Assertions.assertThat;

public class MarkerFlowsTest extends SonarTestCase {

  @Test
  public void display_a_correct_summary_for_secondary_locations_only_flows() {
    var flow1 = new MarkerFlow(0);
    new MarkerFlowLocation(flow1, "message1");
    var flow2 = new MarkerFlow(0);
    new MarkerFlowLocation(flow2, "message2");
    var flow3 = new MarkerFlow(0);
    new MarkerFlowLocation(flow3, "message3");
    var markerFlows = new MarkerFlows(List.of(flow1, flow2, flow3));

    assertThat(markerFlows.getSummaryDescription()).isEqualTo(" [+3 locations]");
  }

  @Test
  public void display_a_correct_summary_for_multiple_flows() {
    var flow1 = new MarkerFlow(0);
    new MarkerFlowLocation(flow1, "message1");
    new MarkerFlowLocation(flow1, "message11");
    var flow2 = new MarkerFlow(0);
    new MarkerFlowLocation(flow2, "message2");
    new MarkerFlowLocation(flow2, "message22");
    var flow3 = new MarkerFlow(0);
    new MarkerFlowLocation(flow3, "message3");
    new MarkerFlowLocation(flow3, "message33");
    var markerFlows = new MarkerFlows(List.of(flow1, flow2, flow3));

    assertThat(markerFlows.getSummaryDescription()).isEqualTo(" [+3 flows]");
  }

  @Test
  public void display_a_correct_summary_for_single_flow() {
    var flow1 = new MarkerFlow(0);
    new MarkerFlowLocation(flow1, "message1");
    new MarkerFlowLocation(flow1, "message11");
    new MarkerFlowLocation(flow1, "message111");
    var markerFlows = new MarkerFlows(List.of(flow1));

    assertThat(markerFlows.getSummaryDescription()).isEqualTo(" [+3 locations]");
  }

}
