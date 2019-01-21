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

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.SelectObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.wizard.WizardPageSupport;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.server.Server;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;

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

    Composite radioButtonGroupContainer = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.makeColumnsEqualWidth = true;
    radioButtonGroupContainer.setLayout(layout);

    Button sonarCloudButton = new Button(radioButtonGroupContainer, SWT.RADIO);
    sonarCloudButton.setImage(SonarLintImages.IMG_SONARCLOUD_LOGO);

    Button onPremiseButton = new Button(radioButtonGroupContainer, SWT.RADIO);
    onPremiseButton.setImage(SonarLintImages.IMG_SONARQUBE_LOGO);

    GridData gd = new GridData(GridData.FILL_BOTH);
    gd.widthHint = 300;
    Link sonarCloudLabel = new Link(radioButtonGroupContainer, SWT.WRAP);
    sonarCloudLabel.setText("Connect to <a>the online service</a>");
    sonarCloudLabel.setLayoutData(gd);
    sonarCloudLabel.addListener(SWT.Selection, e -> {
      try {
        PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(Server.SONARCLOUD_URL));
      } catch (PartInitException | MalformedURLException ex) {
        SonarLintLogger.get().error("Unable to open the browser", ex);
      }
    });
    Label onPremiseLabel = new Label(radioButtonGroupContainer, SWT.WRAP);
    onPremiseLabel.setText("Connect to a server");
    onPremiseLabel.setLayoutData(gd);

    IObservableValue<Boolean> sonarCloudSelection = WidgetProperties.selection().observe(sonarCloudButton);
    IObservableValue<Boolean> onPremSelection = WidgetProperties.selection().observe(onPremiseButton);
    SelectObservableValue<ServerConnectionModel.ConnectionType> selectObservable = new SelectObservableValue<>(ServerConnectionModel.ConnectionType.class);
    selectObservable.addOption(ServerConnectionModel.ConnectionType.SONARCLOUD, sonarCloudSelection);
    selectObservable.addOption(ServerConnectionModel.ConnectionType.ONPREMISE, onPremSelection);
    DataBindingContext dbc = new DataBindingContext();
    dbc.bindValue(selectObservable, PojoProperties.value(ServerConnectionModel.PROPERTY_CONNECTION_TYPE).observe(model));

    WizardPageSupport.create(this, dbc);

    setControl(radioButtonGroupContainer);
  }

}
