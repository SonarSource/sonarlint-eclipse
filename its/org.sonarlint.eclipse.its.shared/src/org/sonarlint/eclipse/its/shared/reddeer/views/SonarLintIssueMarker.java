/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonarlint.eclipse.its.shared.reddeer.views;

import org.eclipse.reddeer.eclipse.ui.markers.AbstractMarker;
import org.eclipse.reddeer.eclipse.ui.views.markers.AbstractMarkersSupportView.Column;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;

/**
 * {@link SonarLintIssueMarker} represents an item in the {@link OnTheFlyView}.
 */
public class SonarLintIssueMarker extends AbstractMarker {

  /**
   * Creates a new problem of Problems view.
   *
   * @param item tree item of a problem
   */
  public SonarLintIssueMarker(TreeItem item) {
    super(item);
  }

  @Override
  protected String getCell(Column column) {
    return getCell(column.toString());
  }

  private String getCell(String column) {
    var onTheFlyView = new OnTheFlyView();
    var columns = onTheFlyView.getProblemColumns();
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

  public void select() {
    markerItem.select();
  }

  public void deactivateRule() {
    markerItem.select();
    new ContextMenuItem(markerItem, "Deactivate rule").select();
  }

  public void delete() {
    markerItem.select();
    new ContextMenuItem(markerItem, "Delete").select();
  }
}
