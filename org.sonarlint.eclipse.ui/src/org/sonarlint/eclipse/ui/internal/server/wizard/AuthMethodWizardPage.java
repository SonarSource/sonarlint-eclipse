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
package org.sonarlint.eclipse.ui.internal.server.wizard;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.SelectObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.wizard.WizardPageSupport;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;

/**
 * Choose between token and login/password authentication mode
 */
public class AuthMethodWizardPage extends WizardPage {

  private final ServerConnectionModel model;

  public AuthMethodWizardPage(ServerConnectionModel model) {
    super("auth_method_page", "Choose authentication method", SonarLintImages.IMG_WIZBAN_NEW_SERVER);
    this.model = model;
  }

  @Override
  public void createControl(Composite parent) {

    Composite radioButtonGroupContainer = new Composite(parent, SWT.NONE);
    radioButtonGroupContainer.setLayout(new GridLayout());

    Button tokenButton = new Button(radioButtonGroupContainer, SWT.RADIO);
    tokenButton.setText("Token");

    Button loginPasswordButton = new Button(radioButtonGroupContainer, SWT.RADIO);
    loginPasswordButton.setText("Username + Password");

    IObservableValue<Boolean> sonarCloudSelection = WidgetProperties.selection().observe(loginPasswordButton);
    IObservableValue<Boolean> onPremSelection = WidgetProperties.selection().observe(tokenButton);
    SelectObservableValue<ServerConnectionModel.AuthMethod> selectObservable = new SelectObservableValue<>(ServerConnectionModel.AuthMethod.class);
    selectObservable.addOption(ServerConnectionModel.AuthMethod.PASSWORD, sonarCloudSelection);
    selectObservable.addOption(ServerConnectionModel.AuthMethod.TOKEN, onPremSelection);
    DataBindingContext dbc = new DataBindingContext();
    dbc.bindValue(selectObservable, PojoProperties.value(ServerConnectionModel.PROPERTY_AUTH_METHOD).observe(model));

    WizardPageSupport.create(this, dbc);

    setControl(radioButtonGroupContainer);
  }

}
