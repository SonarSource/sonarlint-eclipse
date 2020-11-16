package org.sonarlint.eclipse.its.reddeer.views;

import java.util.List;
import org.eclipse.reddeer.eclipse.ui.markers.AbstractMarker;
import org.eclipse.reddeer.eclipse.ui.views.markers.AbstractMarkersSupportView.Column;
import org.eclipse.reddeer.swt.api.TreeItem;

/**
 * SonarLintIssue represents an error or warning in Problems view.
 *
 * @author mlabuda@redhat.com
 * @author rawagner
 * @since 0.7
 */
public class SonarLintIssue extends AbstractMarker {

  /**
   * Creates a new problem of Problems view.
   *
   * @param item tree item of a problem
   */
  public SonarLintIssue(TreeItem item) {
    super(item);
  }

  @Override
  protected String getCell(Column column) {
    OnTheFlyView onTheFlyView = new OnTheFlyView();
    List<String> columns = onTheFlyView.getProblemColumns();
    if (columns.contains(column.toString())) {
      return markerItem.getCell(onTheFlyView.getIndexOfColumn(column));
    }
    return null;
  }

  @Override
  public String toString() {
    return super.toString();
  }
}
