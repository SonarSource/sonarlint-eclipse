/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2019 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.popup;

import org.eclipse.mylyn.commons.ui.dialogs.AbstractNotificationPopup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;

public abstract class AbstractSonarLintPopup extends AbstractNotificationPopup {

  public AbstractSonarLintPopup(Display display) {
    super(display);
  }

  private static final int MAX_WIDTH = 400;
  private static final int MIN_HEIGHT = 100;
  private static final int PADDING_EDGE = 5;

  @Override
  protected void initializeBounds() {
    Rectangle clArea = getPrimaryClientArea();
    // Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=539794
    Point initialSize = getShell().computeSize(MAX_WIDTH, SWT.DEFAULT);
    int height = Math.max(initialSize.y, MIN_HEIGHT);
    int width = Math.min(initialSize.x, MAX_WIDTH);

    Point size = new Point(width, height);
    getShell().setLocation(clArea.width + clArea.x - size.x - PADDING_EDGE, clArea.height + clArea.y - size.y - PADDING_EDGE);
    getShell().setSize(size);
  }

  private Rectangle getPrimaryClientArea() {
    Monitor primaryMonitor = getShell().getDisplay().getPrimaryMonitor();
    return (primaryMonitor != null) ? primaryMonitor.getClientArea() : getShell().getDisplay().getClientArea();
  }
}
