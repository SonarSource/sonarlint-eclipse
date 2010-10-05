/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.tests.common;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IMarker;

/**
 * This class is use to check Marker.
 * 
 * @author Jérémie Lagarde
 */
class MarkerChecker {
  private long   makerId;
  private int    priority;
  private long   line;
  private String message;

  public MarkerChecker(int priority, long line, String message) {
    this.priority = priority;
    this.line = line;
    this.message = message;
  }

  public boolean check(IMarker marker) {
    try {     
      if (line != Long.parseLong(marker.getAttribute(IMarker.LINE_NUMBER).toString()))
        return false;
      if (priority != Integer.parseInt(marker.getAttribute(IMarker.PRIORITY).toString()))
        return false;
      if (!StringUtils.equals(message, marker.getAttribute(IMarker.MESSAGE).toString()))
        return false;
    } catch (Throwable ex) {
      ex.printStackTrace();
      makerId = 0;
      return false;
    }
    makerId = marker.getId();
    return true;
  }

  public boolean isChecked() {
    return makerId != 0;
  }
}
