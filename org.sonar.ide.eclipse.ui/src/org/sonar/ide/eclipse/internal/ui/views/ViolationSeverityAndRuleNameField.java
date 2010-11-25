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

package org.sonar.ide.eclipse.internal.ui.views;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.markers.MarkerField;
import org.eclipse.ui.views.markers.MarkerItem;
import org.sonar.ide.eclipse.ui.SonarUiPlugin;
import org.sonar.ide.shared.violations.ViolationUtils;

/**
 * Each rule in Sonar has severity, so it seems logical to combine rule name and severity in one field.
 */
public class ViolationSeverityAndRuleNameField extends MarkerField {

  private static Image[] images = null;

  @Override
  public int compare(MarkerItem item1, MarkerItem item2) {
    int severity1 = getSeverity(item1);
    int severity2 = getSeverity(item2);
    if (severity1 == severity2) {
      super.compare(item1, item2);
    }
    return severity2 - severity1;
  }

  private int getSeverity(MarkerItem item) {
    return ViolationUtils.convertPriority(item.getMarker().getAttribute("rulepriority", ""));
  }

  @Override
  public String getValue(MarkerItem item) {
    if (item == null || item.getMarker() == null) {
      return null;
    }
    return item.getMarker().getAttribute("rulename", "");
  }

  private Image getImage(MarkerItem item) {
    return getSeverityImage(getSeverity(item));
  }

  private Image getSeverityImage(int severity) {
    if (images == null) {
      images = new Image[5];
      images[0] = ImageDescriptor.createFromFile(SonarUiPlugin.class, "/org/sonar/ide/images/priority/blocker.gif").createImage(); //$NON-NLS-1$
      images[1] = ImageDescriptor.createFromFile(SonarUiPlugin.class, "/org/sonar/ide/images/priority/critical.gif").createImage(); //$NON-NLS-1$
      images[2] = ImageDescriptor.createFromFile(SonarUiPlugin.class, "/org/sonar/ide/images/priority/major.gif").createImage(); //$NON-NLS-1$
      images[3] = ImageDescriptor.createFromFile(SonarUiPlugin.class, "/org/sonar/ide/images/priority/minor.gif").createImage(); //$NON-NLS-1$
      images[4] = ImageDescriptor.createFromFile(SonarUiPlugin.class, "/org/sonar/ide/images/priority/info.gif").createImage(); //$NON-NLS-1$
    }
    return images[severity];
  }

  /**
   * TODO see {@link #annotateImage(MarkerItem, Image)}
   */
  @Override
  public void update(ViewerCell cell) {
    super.update(cell);

    MarkerItem item = (MarkerItem) cell.getElement();
    cell.setImage(getImage(item));
  }

}
