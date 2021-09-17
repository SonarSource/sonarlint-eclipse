/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2021 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.markers;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.views.markers.MarkerItem;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;

public class QuickFixPropertyTester extends PropertyTester {

  private static final String QUICK_FIX = "quickFix"; //$NON-NLS-1$

  /**
   * Create a new instance of the receiver.
   */
  public QuickFixPropertyTester() {
    super();
  }

  @Override
  public boolean test(Object receiver, String property, Object[] args,
    Object expectedValue) {
    if (property.equals(QUICK_FIX)) {
      return !MarkerUtils.getIssueQuickFixes(((MarkerItem) receiver).getMarker()).getQuickFixes().isEmpty();
    }
    return false;
  }

}
