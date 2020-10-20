package org.sonarlint.eclipse.core.internal.markers;

import org.eclipse.core.resources.IMarker;

public class MarkerFlowLocation {
  private final MarkerFlow parent;
  private final int number;
  private final String message;
  private IMarker marker;

  public MarkerFlowLocation(MarkerFlow parent, String message) {
    this.parent = parent;
    this.parent.locations.add(this);
    this.number = this.parent.locations.size();
    this.message = message;
  }

  public MarkerFlow getParent() {
    return parent;
  }

  public int getNumber() {
    return number;
  }

  public String getMessage() {
    return message;
  }

  public void setMarker(IMarker marker) {
    this.marker = marker;
  }

  public IMarker getMarker() {
    return marker;
  }
}
