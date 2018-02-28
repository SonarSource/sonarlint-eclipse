/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2017 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.PropertyPage;

public abstract class AbstractListPropertyPage extends PropertyPage {
  protected Button removeButton;
  protected Button editButton;

  protected Composite createButtons(Composite innerParent) {
    GridLayout layout;
    Composite buttons = new Composite(innerParent, SWT.NONE);
    buttons.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
    layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    buttons.setLayout(layout);

    Button addButton = new Button(buttons, SWT.PUSH);
    addButton.setText("New...");
    addButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    addButton.addListener(SWT.Selection, e -> add());

    editButton = new Button(buttons, SWT.PUSH);
    editButton.setText("Edit...");
    editButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    editButton.addListener(SWT.Selection, e -> edit());

    removeButton = new Button(buttons, SWT.PUSH);
    removeButton.setText("Remove");
    removeButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    removeButton.addListener(SWT.Selection, e -> remove());
    return buttons;
  }

  protected abstract void add();

  protected abstract void edit();

  protected abstract void remove();

}
