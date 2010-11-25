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

package org.sonar.ide.eclipse.ui.util;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

public final class SelectionUtils {

  /**
   * Returns the selected element if the selection consists of a single
   * element only.
   * 
   * @param s the selection
   * @return the selected first element or null
   */
  public static Object getSingleElement(ISelection s) {
    if ( !(s instanceof IStructuredSelection)) {
      return null;
    }
    IStructuredSelection selection = (IStructuredSelection) s;
    if (selection.size() != 1) {
      return null;
    }
    return selection.getFirstElement();
  }

  private SelectionUtils() {
  }

}
