/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.server;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.PlatformUI;

/**
 * A basic cell label provider.
 */
public abstract class BaseCellLabelProvider extends ColumnLabelProvider {
  private ILabelDecorator decorator;
  protected ILabelProviderListener providerListener;

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.viewers.ColumnLabelProvider#update(org.eclipse.jface.viewers.ViewerCell)
   */
  @Override
  public void update(ViewerCell cell) {
    super.update(cell);
    Object element = cell.getElement();
    int index = cell.getColumnIndex();
    cell.setText(getColumnText(element, index));
    Image image = getColumnImage(element, index);
    cell.setImage(image);
  }

  /**
   * Create a BaseCellLabelProvider
   */
  public BaseCellLabelProvider() {
    super();
  }

  /**
   * Create a BaseCellLabelProvider with a decorator at the front of the row
   * @param decorator
   */
  public BaseCellLabelProvider(ILabelDecorator decorator) {
    super();

    this.decorator = getDecorator();
  }

  @Override
  public Point getToolTipShift(Object object) {
    return new Point(5, 5);
  }

  @Override
  public int getToolTipDisplayDelayTime(Object object) {
    return 2000;
  }

  @Override
  public int getToolTipTimeDisplayed(Object object) {
    return 5000;
  }

  @Override
  public void dispose() {
    if (decorator != null && providerListener != null) {
      decorator.removeListener(providerListener);
    }
    super.dispose();
  }

  public ILabelDecorator getDecorator() {
    if (decorator == null) {
      decorator = PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator();
      providerListener = new ILabelProviderListener() {
        @SuppressWarnings("synthetic-access")
        @Override
        public void labelProviderChanged(LabelProviderChangedEvent event) {
          fireLabelProviderChanged(event);
        }
      };
      decorator.addListener(providerListener);
    }
    return decorator;
  }

  /**
   * Extenders of this class would implement this method to provide an image to the column based on the element 
   * being passed
   * @param element
   * @param index
   * @return an image
   */
  public abstract Image getColumnImage(Object element, int index);

  /**
    * Extenders of this class would implement this method to provide a text label to the column based on the element
    * being passed 
   * @param element
   * @param index
   * @return a string
   */
  public abstract String getColumnText(Object element, int index);
}
