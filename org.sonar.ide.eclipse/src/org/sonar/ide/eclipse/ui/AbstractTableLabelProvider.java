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

package org.sonar.ide.eclipse.ui;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;

public abstract class AbstractTableLabelProvider implements ITableLabelProvider {

  /**
   * {@inheritDoc}
   */
  public Image getColumnImage(Object element, int columnIndex) {
    return null;
  }

  public abstract String getColumnText(Object element, int columnIndex);

  /**
   * {@inheritDoc}
   */
  public void addListener(ILabelProviderListener listener) {
  }

  /**
   * {@inheritDoc}
   */
  public void dispose() {
  }

  /**
   * {@inheritDoc}
   */
  public boolean isLabelProperty(Object element, String property) {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public void removeListener(ILabelProviderListener listener) {
  }

}
