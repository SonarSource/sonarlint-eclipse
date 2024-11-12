/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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
import org.sonarlint.eclipse.core.internal.telemetry.LinkTelemetry;
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
    super("connection_type_page", "Choose connection type", SonarLintImages.IMG_WIZBAN_NEW_CONNECTION);
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

    var sonarQubeButton = new Button(radioButtonGroupContainer, SWT.RADIO);
    sonarQubeButton.setImage(SonarLintImages.IMG_SONARQUBE_LOGO);

    var gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.widthHint = 300;
    gd.heightHint = 80;

    var sonarCloudLabel = new Label(radioButtonGroupContainer, SWT.WRAP);
    sonarCloudLabel.setText("A Software-as-a-Service (SaaS) tool that easily integrates into the cloud DevOps platforms "
      + "and extends the CI/CD workflow to systematically help developers and organizations deliver Clean Code.");
    sonarCloudLabel.setLayoutData(gd);

    var sonarQubeLabel = new Label(radioButtonGroupContainer, SWT.WRAP);
    sonarQubeLabel.setText("An Open-source, self-managed tool that easily integrates into the developers' "
      + "CI/CD pipeline and DevOps platform to systematically help developers and organizations deliver Clean Code.");
    sonarQubeLabel.setLayoutData(gd);

    var sonarCloudFreeLabel = new Link(radioButtonGroupContainer, SWT.WRAP);
    sonarCloudFreeLabel.setText("<a>SonarQube Cloud</a> is entirely free for open source projects");
    sonarCloudFreeLabel.setLayoutData(gd);
    sonarCloudFreeLabel.addListener(SWT.Selection, e -> BrowserUtils.openExternalBrowserWithTelemetry(LinkTelemetry.SONARCLOUD_PRODUCT_PAGE, e.display));

    var sonarQubeFreeLabel = new Link(radioButtonGroupContainer, SWT.WRAP);
    sonarQubeFreeLabel.setText("SonarQube Server offers a free <a>Community Build</a>");
    sonarQubeFreeLabel.setLayoutData(gd);
    sonarQubeFreeLabel.addListener(SWT.Selection, e -> BrowserUtils.openExternalBrowserWithTelemetry(LinkTelemetry.SONARQUBE_EDITIONS_DOWNLOADS, e.display));

    var comparisonLabel = new Link(radioButtonGroupContainer, SWT.WRAP);
    comparisonLabel.setText("Discover which option is the best for your team <a>here</a>");
    comparisonLabel.addListener(SWT.Selection, e -> BrowserUtils.openExternalBrowserWithTelemetry(LinkTelemetry.COMPARE_SERVER_PRODUCTS, e.display));
    comparisonLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    var sonarCloudSelection = WidgetPropertiesCompat.buttonSelection().observe(sonarCloudButton);
    var sonarQubeSelection = WidgetPropertiesCompat.buttonSelection().observe(sonarQubeButton);
    var selectObservable = new SelectObservableValue<>(ServerConnectionModel.ConnectionType.class);
    selectObservable.addOption(ServerConnectionModel.ConnectionType.SONARCLOUD, sonarCloudSelection);
    selectObservable.addOption(ServerConnectionModel.ConnectionType.ONPREMISE, sonarQubeSelection);
    var dataBindingContext = new DataBindingContext();
    dataBindingContext.bindValue(selectObservable, PojoPropertiesCompat.value(ServerConnectionModel.PROPERTY_CONNECTION_TYPE).observe(model));

    WizardPageSupport.create(this, dataBindingContext);

    setControl(radioButtonGroupContainer);
  }

}
