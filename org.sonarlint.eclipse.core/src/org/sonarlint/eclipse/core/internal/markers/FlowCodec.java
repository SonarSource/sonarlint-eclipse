/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2019 SonarSource SA
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

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue.Flow;
import org.sonarsource.sonarlint.core.client.api.common.analysis.IssueLocation;

public class FlowCodec {

  /*
   * The separators below were chosen because:
   * - They should not appear in issue messages (non-printable, control characters)
   * - They neatly encode to a single byte in UTF-8
   */
  private static final String SEPARATOR_FLOWS     = "\u0011";
  private static final String SEPARATOR_LOCATIONS = "\u0012";
  private static final String SEPARATOR_LOCATION  = "\u0013";

  private FlowCodec() {
    // Utility class
  }

  @Nonnull
  public static String encode(Collection<Flow> flows) {
    return flows.stream().map(FlowCodec::encode).collect(Collectors.joining(SEPARATOR_FLOWS));
  }

  private static String encode(Flow flow) {
    return flow.locations().stream().map(FlowCodec::encode).collect(Collectors.joining(SEPARATOR_LOCATIONS));
  }

  private static String encode(IssueLocation location) {
    return Stream.of(
      location.getMessage(),
      Integer.toString(location.getStartLine()),
      Integer.toString(location.getStartLineOffset()),
      Integer.toString(location.getEndLine()),
      Integer.toString(location.getEndLineOffset())).collect(Collectors.joining(SEPARATOR_LOCATION));
  }

  public static List<Flow> decode(String encodedFlows) {
    return Stream.of(encodedFlows.split(SEPARATOR_FLOWS))
      .filter(encodedFlow -> !encodedFlow.isEmpty())
      .map(DecodedFlow::new)
      .filter(decodedFlow -> !decodedFlow.locations.isEmpty())
      .collect(Collectors.toList());
  }

  public static class DecodedFlow implements Flow {

    private final List<IssueLocation> locations;

    private DecodedFlow(String encodedFlow) {
      locations = Stream.of(encodedFlow.split(SEPARATOR_LOCATIONS))
        .map(encodedLocation -> encodedLocation.split(SEPARATOR_LOCATION))
        .filter(attributes -> attributes.length == 5)
        .map(DecodedLocation::new)
        .collect(Collectors.toList());
    }

    @Override
    public List<IssueLocation> locations() {
      return locations;
    }

  }

  public static class DecodedLocation implements IssueLocation {

    private String message;
    private int startLine;
    private int startLineOffset;
    private int endLine;
    private int endLineOffset;

    private DecodedLocation(String[] attributes) {
      message = attributes[0];
      startLine = Integer.valueOf(attributes[1], 10);
      startLineOffset = Integer.valueOf(attributes[2], 10);
      endLine = Integer.valueOf(attributes[3], 10);
      endLineOffset = Integer.valueOf(attributes[4], 10);
    }

    @Override
    public ClientInputFile getInputFile() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Integer getStartLine() {
      return startLine;
    }

    @Override
    public Integer getEndLine() {
      return endLine;
    }

    @Override
    public Integer getStartLineOffset() {
      return startLineOffset;
    }

    @Override
    public Integer getEndLineOffset() {
      return endLineOffset;
    }

    @Override
    public String getMessage() {
      return message;
    }
  }
}
