package org.sonar.ide.eclipse.ui.internal.views.issues;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.views.markers.MarkerFieldFilter;
import org.eclipse.ui.views.markers.MarkerItem;
import org.sonar.ide.eclipse.core.internal.markers.MarkerUtils;

public class IsNewIssueFieldFilter extends MarkerFieldFilter {

  public static final int NEW = 1;
  public static final int OTHER = 0;

  final static int SHOW_NEW = 1 << NEW;
  final static int SHOW_OTHER = 1 << OTHER;

  private static final String TAG_SELECTED_NEW = "selectedNewIssues"; //$NON-NLS-1$

  int selectedNewIssues = SHOW_NEW + SHOW_OTHER;

  /**
   * Create a new instance of the receiver
   */
  public IsNewIssueFieldFilter() {
    super();
  }

  public void loadSettings(IMemento memento) {
    Integer showNew = memento.getInteger(TAG_SELECTED_NEW);
    if (showNew == null)
      return;
    selectedNewIssues = showNew.intValue();
  }

  public void saveSettings(IMemento memento) {
    memento.putInteger(TAG_SELECTED_NEW, selectedNewIssues);
  }

  public boolean select(MarkerItem item) {

    if (selectedNewIssues == 0)
      return true;
    IMarker marker = item.getMarker();
    if (marker == null)
      return false;
    int markerIsNew = 1 << (marker.getAttribute(MarkerUtils.SONAR_MARKER_IS_NEW_ATTR, false) ? NEW : OTHER);

    switch (markerIsNew) {
      case SHOW_NEW:
        return (selectedNewIssues & SHOW_NEW) > 0;
      case SHOW_OTHER:
        return (selectedNewIssues & SHOW_OTHER) > 0;
      default:
        return true;
    }

  }

  public void populateWorkingCopy(MarkerFieldFilter copy) {
    super.populateWorkingCopy(copy);
    ((IsNewIssueFieldFilter) copy).selectedNewIssues = selectedNewIssues;
  }
}
