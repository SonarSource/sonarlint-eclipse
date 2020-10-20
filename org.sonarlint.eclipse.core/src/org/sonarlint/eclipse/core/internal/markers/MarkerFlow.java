package org.sonarlint.eclipse.core.internal.markers;

import java.util.ArrayList;
import java.util.List;

public class MarkerFlow {
  private final int number;
  final List<MarkerFlowLocation> locations = new ArrayList<>();

  public MarkerFlow(int number) {
    this.number = number;
  }

  public int getNumber() {
    return number;
  }

  public List<MarkerFlowLocation> getLocations() {
    return locations;
  }

}