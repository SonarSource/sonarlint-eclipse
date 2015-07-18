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
package org.sonar.ide.eclipse.ui.internal;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;

public abstract class AbstractTableLabelProvider implements ITableLabelProvider {

  /**
   * {@inheritDoc}
   */
  @Override
  public Image getColumnImage(Object element, int columnIndex) {
    return null;
  }

  @Override
  public abstract String getColumnText(Object element, int columnIndex);

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(ILabelProviderListener listener) {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispose() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isLabelProperty(Object element, String property) {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(ILabelProviderListener listener) {
  }

}
