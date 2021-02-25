/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2021 SonarSource SA
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
    return getCell(column.toString());
  }

  private String getCell(String column) {
    OnTheFlyView onTheFlyView = new OnTheFlyView();
    List<String> columns = onTheFlyView.getProblemColumns();
    if (columns.contains(column)) {
      return markerItem.getCell(onTheFlyView.getIndexOfColumn(column));
    }
    return null;
  }

  @Override
  public String toString() {
    return super.toString();
  }

  public String getCreationDate() {
    return getCell("Date");
  }
}
