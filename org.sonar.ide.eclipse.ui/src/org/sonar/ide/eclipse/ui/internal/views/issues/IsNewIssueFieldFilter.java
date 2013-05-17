/*
 * Sonar Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.ui.internal.views.issues;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.views.markers.MarkerFieldFilter;
import org.eclipse.ui.views.markers.MarkerItem;
import org.sonar.ide.eclipse.core.internal.markers.MarkerUtils;

import java.util.Map;

public class IsNewIssueFieldFilter extends MarkerFieldFilter {

  public static final int NEW = 1;
  public static final int OTHER = 0;

  static final int SHOW_NEW = 1 << NEW;
  static final int SHOW_OTHER = 1 << OTHER;

  static final String TAG_SELECTED_NEW = "selectedNewIssues"; //$NON-NLS-1$

  int selectedNewIssues = SHOW_NEW + SHOW_OTHER;

  /**
   * Create a new instance of the receiver
   */
  public IsNewIssueFieldFilter() {
    super();
  }

  public void loadSettings(IMemento memento) {
    Integer showNew = memento.getInteger(TAG_SELECTED_NEW);
    if (showNew == null) {
      return;
    }
    selectedNewIssues = showNew.intValue();
  }

  public void saveSettings(IMemento memento) {
    memento.putInteger(TAG_SELECTED_NEW, selectedNewIssues);
  }

  public boolean select(MarkerItem item) {

    if (selectedNewIssues == 0) {
      return true;
    }
    IMarker marker = item.getMarker();
    if (marker == null) {
      return false;
    }
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

  @Override
  public void initialize(Map values) {
    if (values.containsKey(TAG_SELECTED_NEW)) {
      selectedNewIssues = (Integer) values.get(TAG_SELECTED_NEW);
    }
  }

  public void populateWorkingCopy(MarkerFieldFilter copy) {
    super.populateWorkingCopy(copy);
    ((IsNewIssueFieldFilter) copy).selectedNewIssues = selectedNewIssues;
  }
}
