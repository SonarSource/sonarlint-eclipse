/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2021 SonarSource SA
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.PlatformUI;
import org.sonarlint.eclipse.ui.internal.notifications.AbstractNotificationPopup;

public abstract class AbstractSonarLintPopup extends AbstractNotificationPopup {

  protected AbstractSonarLintPopup() {
    super(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
    setDelayClose(0);
  }

  private Composite linksContainer;

  @Override
  protected void createContentArea(Composite composite) {
    Label messageLabel = new Label(composite, SWT.WRAP);
    GridData messageLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
    messageLabel.setLayoutData(messageLayoutData);

    messageLabel.setText(getMessage());

    linksContainer = new Composite(composite, SWT.NONE);
    GridData linksLayoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
    linksLayoutData.horizontalAlignment = SWT.END;
    linksLayoutData.verticalAlignment = SWT.BOTTOM;
    linksContainer.setLayoutData(linksLayoutData);

    RowLayout rowLayout = new RowLayout();
    rowLayout.spacing = 20;
    linksContainer.setLayout(rowLayout);
  }

  protected abstract String getMessage();

  protected void addLink(String text, Consumer<SelectionEvent> selectionHandler) {
    Link detailsLink = new Link(linksContainer, SWT.NONE);
    detailsLink.setText("<a>" + text + "</a>");
    detailsLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        selectionHandler.accept(e);
      }
    });
  }
}
