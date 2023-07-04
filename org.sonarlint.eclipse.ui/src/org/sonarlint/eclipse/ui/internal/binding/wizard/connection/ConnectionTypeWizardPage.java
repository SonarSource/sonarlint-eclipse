/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2023 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.binding.wizard.connection;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.SelectObservableValue;
import org.eclipse.jface.databinding.wizard.WizardPageSupport;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.util.BrowserUtils;
import org.sonarlint.eclipse.ui.internal.util.wizard.PojoPropertiesCompat;
import org.sonarlint.eclipse.ui.internal.util.wizard.WidgetPropertiesCompat;

/**
 * Choose between SonarCloud and SonarQube on premise
 */
public class ConnectionTypeWizardPage extends WizardPage {

  private final ServerConnectionModel model;

  public ConnectionTypeWizardPage(ServerConnectionModel model) {
    super("server_type_page", "Choose connection type", SonarLintImages.IMG_WIZBAN_NEW_SERVER);
    this.model = model;
  }

  @Override
  public void createControl(Composite parent) {

    var radioButtonGroupContainer = new Composite(parent, SWT.NONE);
    var layout = new GridLayout();
    layout.numColumns = 2;
    layout.makeColumnsEqualWidth = true;
    radioButtonGroupContainer.setLayout(layout);

    var sonarCloudButton = new Button(radioButtonGroupContainer, SWT.RADIO);
    sonarCloudButton.setImage(SonarLintImages.IMG_SONARCLOUD_LOGO);

    var onPremiseButton = new Button(radioButtonGroupContainer, SWT.RADIO);
    onPremiseButton.setImage(SonarLintImages.IMG_SONARQUBE_LOGO);

    var gd = new GridData(GridData.FILL_BOTH);
    gd.widthHint = 300;
    var sonarCloudLabel = new Link(radioButtonGroupContainer, SWT.WRAP);
    sonarCloudLabel.setText("Connect to <a>the online service</a>");
    sonarCloudLabel.setLayoutData(gd);
    sonarCloudLabel.addListener(SWT.Selection, e -> BrowserUtils.openExternalBrowser(SonarLintUtils.getSonarCloudUrl(), e.display));
    var onPremiseLabel = new Label(radioButtonGroupContainer, SWT.WRAP);
    onPremiseLabel.setText("Connect to a server");
    onPremiseLabel.setLayoutData(gd);

    var sonarCloudSelection = WidgetPropertiesCompat.buttonSelection().observe(sonarCloudButton);
    var onPremSelection = WidgetPropertiesCompat.buttonSelection().observe(onPremiseButton);
    var selectObservable = new SelectObservableValue<>(ServerConnectionModel.ConnectionType.class);
    selectObservable.addOption(ServerConnectionModel.ConnectionType.SONARCLOUD, sonarCloudSelection);
    selectObservable.addOption(ServerConnectionModel.ConnectionType.ONPREMISE, onPremSelection);
    var dataBindingContext = new DataBindingContext();
    dataBindingContext.bindValue(selectObservable, PojoPropertiesCompat.value(ServerConnectionModel.PROPERTY_CONNECTION_TYPE).observe(model));

    WizardPageSupport.create(this, dataBindingContext);

    setControl(radioButtonGroupContainer);
  }

}
