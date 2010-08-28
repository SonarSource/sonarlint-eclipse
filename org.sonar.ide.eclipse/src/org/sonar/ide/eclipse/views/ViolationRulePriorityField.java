/*
 * Copyright (C) 2010 Evgeny Mandrikov
 *
 * Sonar-IDE is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar-IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar-IDE; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.views;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.views.markers.MarkerField;
import org.eclipse.ui.views.markers.MarkerItem;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.shared.violations.ViolationUtils;

/**
 * @author Jérémie Lagarde
 */
public class ViolationRulePriorityField extends MarkerField {

  private static Image[] images = null;

  @Override
  public int compare(MarkerItem item1, MarkerItem item2) {
    return -(ViolationUtils.convertPriority(item1.getMarker().getAttribute("rulepriority", ""))-
        ViolationUtils.convertPriority(item2.getMarker().getAttribute("rulepriority", "")));
  }

  @Override
  public String getColumnHeaderText() {
    return "";
  }

  @Override
  public String getColumnTooltipText() {
    return "Priority";
  }

  @Override
  public int getDefaultColumnWidth(Control control) {
    return getPriorityImage("info").getBounds().width;
  }

  private Image getPriorityImage(MarkerItem item) {
    return getPriorityImage(item.getMarker().getAttribute("rulepriority", ""));
  }

  private Image getPriorityImage(String priority) {
    if(images == null) {
      images = new Image[5];
      images[0] = ImageDescriptor.createFromFile(SonarPlugin.class, "/org/sonar/ide/images/priority/blocker.gif").createImage(); //$NON-NLS-1$
      images[1] = ImageDescriptor.createFromFile(SonarPlugin.class, "/org/sonar/ide/images/priority/critical.gif").createImage(); //$NON-NLS-1$
      images[2] = ImageDescriptor.createFromFile(SonarPlugin.class, "/org/sonar/ide/images/priority/major.gif").createImage(); //$NON-NLS-1$
      images[3] = ImageDescriptor.createFromFile(SonarPlugin.class, "/org/sonar/ide/images/priority/minor.gif").createImage(); //$NON-NLS-1$
      images[4] = ImageDescriptor.createFromFile(SonarPlugin.class, "/org/sonar/ide/images/priority/info.gif").createImage(); //$NON-NLS-1$
    }
    return images[ViolationUtils.convertPriority(priority)];
  }

  @Override
  public String getValue(MarkerItem item) {
    return "";
  }

  @Override
  public void update(ViewerCell cell) {
    super.update(cell);
    try {
      Image image = getPriorityImage((MarkerItem) cell.getElement());
      cell.setImage(image);
    } catch (NumberFormatException e) {
      return;
    }
  }
}