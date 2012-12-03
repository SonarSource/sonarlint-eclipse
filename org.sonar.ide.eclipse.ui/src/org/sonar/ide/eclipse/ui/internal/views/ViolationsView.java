/*
 * Sonar Eclipse
 * Copyright (C) 2010-2012 SonarSource
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
package org.sonar.ide.eclipse.ui.internal.views;

import org.eclipse.ui.views.markers.MarkerSupportView;
import org.sonar.ide.eclipse.ui.internal.ISonarConstants;

public class ViolationsView extends MarkerSupportView {

  public static final String ID = ISonarConstants.PLUGIN_ID + ".views.ViolationsView";

  public ViolationsView() {
    super(ISonarConstants.PLUGIN_ID + ".markers.violationMarkerGenerator");
  }

  /**
   * TODO quote from {@link org.eclipse.ui.part.WorkbenchPart#getContentDescription()} : "It is considered bad practice to overload or extend this method."
   */
  @Override
  public String getContentDescription() {
    // TODO : add some metrics about violation makers.
    return "";
  }

}
