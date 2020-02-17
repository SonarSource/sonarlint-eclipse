/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2020 SonarSource SA
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

import java.util.function.Consumer;
import org.eclipse.mylyn.commons.ui.dialogs.AbstractNotificationPopup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Monitor;

public abstract class AbstractSonarLintPopup extends AbstractNotificationPopup {

  public AbstractSonarLintPopup(Display display) {
    super(display);
  }

  private static final int MAX_WIDTH = 400;
  private static final int MIN_HEIGHT = 100;
  private static final int PADDING_EDGE = 5;
  private Composite links;

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

  @Override
  protected void createContentArea(Composite composite) {
    Label messageLabel = new Label(composite, SWT.WRAP);
    GridData messageLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
    messageLabel.setLayoutData(messageLayoutData);

    messageLabel.setText(getMessage());
    messageLabel.setBackground(composite.getBackground());

    links = new Composite(composite, SWT.NONE);
    GridData linksLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
    linksLayoutData.horizontalAlignment = GridData.HORIZONTAL_ALIGN_END;
    links.setLayoutData(linksLayoutData);

    RowLayout rowLayout = new RowLayout();
    rowLayout.spacing = 20;
    links.setLayout(rowLayout);
  }

  protected abstract String getMessage();

  protected void addLink(String text, Consumer<SelectionEvent> selectionHandler) {
    Link detailsLink = new Link(links, SWT.NONE);
    detailsLink.setText("<a>" + text + "</a>");
    detailsLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        selectionHandler.accept(e);
      }
    });
  }
}
