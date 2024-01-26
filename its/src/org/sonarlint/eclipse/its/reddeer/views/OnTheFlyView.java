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
package org.sonarlint.eclipse.its.reddeer.views;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.reddeer.common.exception.RedDeerException;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.eclipse.condition.AbstractExtendedMarkersViewIsUpdating;
import org.eclipse.reddeer.eclipse.exception.EclipseLayerException;
import org.eclipse.reddeer.eclipse.ui.markers.matcher.AbstractMarkerMatcher;
import org.eclipse.reddeer.eclipse.ui.views.markers.AbstractMarkersSupportView;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.impl.tree.DefaultTree;
import org.eclipse.reddeer.workbench.impl.menu.WorkbenchPartMenuItem;
import org.sonarlint.eclipse.ui.internal.views.issues.OnTheFlyIssuesView;

/**
 * Represents the SonarLint On-The-Fly view.
 *
 */
public class OnTheFlyView extends AbstractMarkersSupportView {

  public OnTheFlyView() {
    super("SonarLint On-The-Fly");
  }

  public List<TreeItem> getItems() {
    activate();
    new WaitUntil(new OnTheFlyIssuesViewMarkerIsUpdating(), TimePeriod.MEDIUM, false);
    new WaitWhile(new OnTheFlyIssuesViewMarkerIsUpdating());
    return new DefaultTree(cTabItem).getItems();
  }

  public DefaultTree getTree() {
    activate();
    return new DefaultTree(cTabItem);
  }

  public List<SonarLintIssueMarker> getIssues(AbstractMarkerMatcher... matchers) {
    activate();
    new WaitUntil(new OnTheFlyIssuesViewMarkerIsUpdating(), TimePeriod.SHORT, false);
    new WaitWhile(new OnTheFlyIssuesViewMarkerIsUpdating());

    return getMarkers(matchers);
  }

  public void selectItem(int index) {
    getIssues().get(index).select();
  }

  public void groupByImpact() {
    this.activate();
    new WorkbenchPartMenuItem("Group By", "Impact").select();
  }

  public void groupBySeverityLegacy() {
    this.activate();
    new WorkbenchPartMenuItem("Group By", "Severity (Legacy)").select();
  }

  public void resetGrouping() {
    this.activate();
    new WorkbenchPartMenuItem("Group By", "None").select();
  }

  /**
   * Overrides {@link #getMarkers(Class, String, AbstractMarkerMatcher...)} to remove the filter on markerType that doesn't work for us.
   */
  private List<SonarLintIssueMarker> getMarkers(AbstractMarkerMatcher... matchers) {
    var filteredResult = new ArrayList<SonarLintIssueMarker>();
    var markerItems = new DefaultTree(cTabItem).getItems();
    if (markerItems != null) {
      for (var markerItem : markerItems) {
        if (matchMarkerTreeItem(markerItem, matchers)) {
          try {
            filteredResult.add(new SonarLintIssueMarker(markerItem));
          } catch (IllegalArgumentException | SecurityException e) {
            // if something bad happen, print stack trace and throw RedDeer Exception
            e.printStackTrace();
            throw new EclipseLayerException("Cannot create a new marker.");
          }
        }
      }
    }
    return filteredResult;
  }

  // Copied from parent since it is private
  private boolean matchMarkerTreeItem(TreeItem item, AbstractMarkerMatcher... matchers) {
    var itemFitsMatchers = true;
    if (matchers != null) {
      for (var matcher : matchers) {
        try {
          if (!matcher.matches(item.getCell(getIndexOfColumn(matcher.getColumn())))) {
            itemFitsMatchers = false;
            break;
          }
        } catch (RedDeerException ex) {
          // if widget is disposed we can ignore it - problem disappeared
          if (!item.isDisposed()) {
            throw ex;
          } else {
            itemFitsMatchers = false;
          }
        }
      }
    }
    return itemFitsMatchers;
  }

  private class OnTheFlyIssuesViewMarkerIsUpdating extends AbstractExtendedMarkersViewIsUpdating {

    public OnTheFlyIssuesViewMarkerIsUpdating() {
      super(OnTheFlyView.this, OnTheFlyIssuesView.class);
    }
  }
}
