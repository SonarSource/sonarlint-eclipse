/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2020 SonarSource SA
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
import org.sonarlint.eclipse.ui.internal.views.issues.OnTheFlyIssuesView;

/**
 * Represents the SonarLint On-The-Fly view.
 *
 */
public class OnTheFlyView extends AbstractMarkersSupportView {

  public OnTheFlyView() {
    super("SonarLint On-The-Fly");
  }
  
  public DefaultTree getTree() {
    activate();
    return new DefaultTree(cTabItem);
  }

  public List<TreeItem> getItems() {
    activate();
    new WaitUntil(new OnTheFlyIssuesViewMarkerIsUpdating(), TimePeriod.SHORT, false);
    new WaitWhile(new OnTheFlyIssuesViewMarkerIsUpdating());
    return new DefaultTree(cTabItem).getItems();
  }
  

  public List<SonarLintIssue> getIssues(AbstractMarkerMatcher... matchers) {
    activate();
    new WaitUntil(new OnTheFlyIssuesViewMarkerIsUpdating(), TimePeriod.SHORT, false);
    new WaitWhile(new OnTheFlyIssuesViewMarkerIsUpdating());

    List<SonarLintIssue> result = new ArrayList<>();
    result.addAll(getMarkers(matchers));
    return result;
  }
  
  public void selectItem(int index) {
    activate();
    new WaitUntil(new OnTheFlyIssuesViewMarkerIsUpdating(), TimePeriod.SHORT, false);
    new WaitWhile(new OnTheFlyIssuesViewMarkerIsUpdating());
    new DefaultTree(cTabItem).getItems().get(index).select();
  }

  protected List<SonarLintIssue> getMarkers(AbstractMarkerMatcher... matchers) {
    List<SonarLintIssue> filteredResult = new ArrayList<>();
    List<TreeItem> markerItems = new DefaultTree(cTabItem).getItems();
    if (markerItems != null) {
      for (TreeItem markerItem : markerItems) {
        if (matchMarkerTreeItem(markerItem, matchers)) {
          try {
            filteredResult.add(new SonarLintIssue(markerItem));
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

  private boolean matchMarkerTreeItem(TreeItem item, AbstractMarkerMatcher... matchers) {
    boolean itemFitsMatchers = true;
    if (matchers != null) {
      for (AbstractMarkerMatcher matcher : matchers) {
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
