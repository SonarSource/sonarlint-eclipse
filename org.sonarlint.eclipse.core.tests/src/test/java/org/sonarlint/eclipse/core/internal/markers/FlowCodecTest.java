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
package org.sonarlint.eclipse.core.internal.markers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue.Flow;
import org.sonarsource.sonarlint.core.client.api.common.analysis.IssueLocation;

public class FlowCodecTest {

  @Test
  public void shouldEncodeEmptyFlows() throws Exception {
    Flow flow = mockFlow();
    assertThat(FlowCodec.encode(Collections.singletonList(flow))).isEqualTo("");
  }

  @Test
  public void shouldDecodeEmptyFlows() throws Exception {
    assertThat(FlowCodec.decode("")).isEmpty();
  }

  @Test
  public void shouldIgnoreGibberish() throws Exception {
    assertThat(FlowCodec.decode("Definitely not a well formed flow")).isEmpty();
  }

  @Test
  public void shouldEncodeOneFlow() throws Exception {
    IssueLocation location = mockLocation("This is a <message>, at this location!", 0, 1, 2, 3);
    Flow flow = mockFlow(location);
    String encoded = FlowCodec.encode(Collections.singletonList(flow));
    assertThat(encoded).isEqualTo("This is a <message>, at this location!\u00130\u00131\u00132\u00133");
    assertThat(encoded.getBytes(StandardCharsets.UTF_8)).hasSize(
      38 /* 'This is a <message>, at this location!' */
      + 1 /* separator */ + 1 /* 0 */
      + 1 /* separator */ + 1 /* 1 */
      + 1 /* separator */ + 1 /* 2 */
      + 1 /* separator */ + 1 /* 3 */
    );
  }

  @Test
  public void shouldDecodeOneFlow() throws Exception {
    List<Flow> flows = FlowCodec.decode("This is a <message>, at this location!\u00130\u00131\u00132\u00133");
    assertThat(flows).hasSize(1);
    Flow flow = flows.get(0);
    assertThat(flow.locations())
      .extracting("message", "startLine", "startLineOffset", "endLine", "endLineOffset")
      .containsExactly(tuple("This is a <message>, at this location!", 0, 1, 2, 3));
    try {
      flow.locations().get(0).getInputFile();
      Assertions.failBecauseExceptionWasNotThrown(UnsupportedOperationException.class);
    } catch (Exception expected) {
      assertThat(expected).isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Test
  public void shouldEncodeMultipleFlows() throws Exception {
    IssueLocation location11 = mockLocation("Location 1 1", 1, 0, 1, 10);
    IssueLocation location12 = mockLocation("Location 1 2", 2, 0, 2, 10);
    Flow flow1 = mockFlow(location11, location12);
    IssueLocation location21 = mockLocation("Location 2 1", 3, 0, 3, 10);
    IssueLocation location22 = mockLocation("Location 2 2", 4, 0, 4, 10);
    Flow flow2 = mockFlow(location21, location22);
    String output = FlowCodec.encode(Arrays.asList(flow1, flow2));
    String expected = "Location 1 1\u00131\u00130\u00131\u001310"
      + "\u0012"
      + "Location 1 2\u00132\u00130\u00132\u001310"
      + "\u0011"
      + "Location 2 1\u00133\u00130\u00133\u001310"
      + "\u0012"
      + "Location 2 2\u00134\u00130\u00134\u001310";
    assertThat(output).isEqualTo(expected);
  }

  @Test
  public void shouldDecodeMultipleFlows() throws Exception {
    String encoded = "Location 1 1\u00131\u00130\u00131\u001310"
      + "\u0012"
      + "Location 1 2\u00132\u00130\u00132\u001310"
      + "\u0011"
      + "Location 2 1\u00133\u00130\u00133\u001310"
      + "\u0012"
      + "Location 2 2\u00134\u00130\u00134\u001310";
    List<Flow> decodedFlows = FlowCodec.decode(encoded);
    assertThat(decodedFlows).hasSize(2);
    Flow flow1 = decodedFlows.get(0);
    assertThat(flow1.locations())
      .extracting("message", "startLine", "startLineOffset", "endLine", "endLineOffset")
      .containsExactly(
        tuple("Location 1 1", 1, 0, 1, 10),
        tuple("Location 1 2", 2, 0, 2, 10));
    Flow flow2 = decodedFlows.get(1);
    assertThat(flow2.locations())
    .extracting("message", "startLine", "startLineOffset", "endLine", "endLineOffset")
    .containsExactly(
      tuple("Location 2 1", 3, 0, 3, 10),
      tuple("Location 2 2", 4, 0, 4, 10));
  }

  private static Flow mockFlow(IssueLocation... locations) {
    Flow flow = mock(Flow.class);
    when(flow.locations()).thenReturn(Arrays.asList(locations));
    return flow;
  }

  private static IssueLocation mockLocation(String message, int startLine, int startLineOffset, int endline, int endLineOffset) {
    IssueLocation location = mock(IssueLocation.class);
    when(location.getMessage()).thenReturn(message);
    when(location.getStartLine()).thenReturn(startLine);
    when(location.getStartLineOffset()).thenReturn(startLineOffset);
    when(location.getEndLine()).thenReturn(endline);
    when(location.getEndLineOffset()).thenReturn(endLineOffset);
    return location;
  }
}
