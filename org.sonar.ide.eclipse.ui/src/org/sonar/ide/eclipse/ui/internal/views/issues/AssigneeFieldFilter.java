/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
 * sonarqube@googlegroups.com
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

import org.apache.commons.lang.StringUtils;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.views.markers.MarkerFieldFilter;
import org.eclipse.ui.views.markers.MarkerItem;
import org.eclipse.ui.views.markers.MarkerSupportConstants;
import org.sonar.ide.eclipse.core.internal.markers.MarkerUtils;

import java.util.Map;

public class AssigneeFieldFilter extends MarkerFieldFilter {

  static final String TAG_CONTAINS_MODIFIER = "assigneeContainsModifier"; //$NON-NLS-1$
  static final String TAG_CONTAINS_TEXT = "assigneeContainsText"; //$NON-NLS-1$

  String containsModifier = MarkerSupportConstants.CONTAINS_KEY;
  String containsText = ""; //$NON-NLS-1$

  /**
   * Create a new instance of the receiver.
   */
  public AssigneeFieldFilter() {
    super();
  }

  @Override
  public void loadSettings(IMemento memento) {
    String modifier = memento.getString(TAG_CONTAINS_MODIFIER);
    if (modifier == null) {
      return;
    }
    String contains = memento.getString(TAG_CONTAINS_TEXT);
    if (contains == null) {
      return;
    }
    containsText = contains;
    containsModifier = modifier;

  }

  @Override
  public void saveSettings(IMemento memento) {
    memento.putString(TAG_CONTAINS_MODIFIER, containsModifier);
    memento.putString(TAG_CONTAINS_TEXT, containsText);
  }

  @Override
  public boolean select(MarkerItem item) {

    String value = item.getAttributeValue(MarkerUtils.SONAR_MARKER_ASSIGNEE, "");
    if (containsModifier.equals(MarkerSupportConstants.CONTAINS_KEY)) {
      if (containsText.length() == 0) {
        return true;
      }
      return value.indexOf(containsText) >= 0;
    } else {
      if (containsText.length() == 0) {
        return StringUtils.isNotBlank(value);
      }
      return value.indexOf(containsText) < 0;
    }
  }

  @Override
  public void populateWorkingCopy(MarkerFieldFilter copy) {
    super.populateWorkingCopy(copy);
    AssigneeFieldFilter clone = (AssigneeFieldFilter) copy;
    clone.containsModifier = this.containsModifier;
    clone.containsText = this.containsText;
  }

  /**
   * Return the contains modifier.
   *
   * @return One of {@link MarkerSupportConstants#CONTAINS_KEY} or
   *         {@link MarkerSupportConstants#DOES_NOT_CONTAIN_KEY}
   */
  String getContainsModifier() {
    return containsModifier;
  }

  /**
   * Set the contains modifier.
   *
   * @param containsString
   *            One of {@link MarkerSupportConstants#CONTAINS_KEY} or
   *            {@link MarkerSupportConstants#DOES_NOT_CONTAIN_KEY}
   */
  void setContainsModifier(String containsString) {
    this.containsModifier = containsString;
  }

  /**
   * Return the text to apply the containsModifier to.
   *
   * @return String
   */
  String getContainsText() {
    return containsText;
  }

  /**
   * Set the text to apply the containsModifier to.
   *
   * @param containsText
   *            String
   */
  void setContainsText(String containsText) {
    this.containsText = containsText;
  }

  @Override
  public void initialize(Map values) {
    super.initialize(values);
    if (values.containsKey(TAG_CONTAINS_MODIFIER)) {
      setContainsModifier((String) values.get(TAG_CONTAINS_MODIFIER));
    }
    if (values
      .containsKey(TAG_CONTAINS_TEXT)) {
      setContainsText((String) values.get(TAG_CONTAINS_TEXT));
    }
  }

}
