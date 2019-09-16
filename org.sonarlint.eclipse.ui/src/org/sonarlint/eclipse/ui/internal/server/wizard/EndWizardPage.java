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
package org.sonarlint.eclipse.ui.internal.server.wizard;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.wizard.WizardPageSupport;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;

public class EndWizardPage extends WizardPage {

  private final ServerConnectionModel model;
  private Button notificationsEnabledCheckbox;

  public EndWizardPage(ServerConnectionModel model) {
    super("server_end_page", "Configuration completed", SonarLintImages.IMG_WIZBAN_NEW_SERVER);
    this.model = model;
  }

  @Override
  public void createControl(Composite parent) {

    Composite container = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    container.setLayout(layout);

    GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
    container.setLayoutData(layoutData);

    Label label = new Label(container, SWT.WRAP);
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    label.setLayoutData(gd);
    if (model.isEdit()) {
      label.setText("Connection successfully edited. Click finish to save your changes and schedule an update of all project bindings.");
    } else {
      label.setText("Connection successfully created. Click finish to save and schedule an update of all project bindings.");
    }

    notificationsEnabledCheckbox = new Button(container, SWT.CHECK);
    notificationsEnabledCheckbox.setText("Receive notifications about events in this server");
    notificationsEnabledCheckbox.setVisible(false);

    DataBindingContext dbc = new DataBindingContext();
    dbc.bindValue(
      WidgetProperties.selection().observe(notificationsEnabledCheckbox),
      BeanProperties.value(ServerConnectionModel.class, ServerConnectionModel.PROPERTY_NOTIFICATIONS_ENABLED)
        .observe(model),
      null,
      null);

    WizardPageSupport.create(this, dbc);

    setControl(container);
  }

  public void setNotificationsSupported(boolean notificationsSupported) {
    notificationsEnabledCheckbox.setVisible(notificationsSupported);
  }

}
