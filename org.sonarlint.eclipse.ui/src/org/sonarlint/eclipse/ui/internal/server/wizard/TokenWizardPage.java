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
package org.sonarlint.eclipse.ui.internal.server.wizard;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.wizard.WizardPageSupport;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.ui.internal.Messages;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;

public class TokenWizardPage extends WizardPage {

  private final ServerConnectionModel model;

  private Text serverTokenText;

  public TokenWizardPage(ServerConnectionModel model) {
    super("server_token_page", "SonarQube Server Authentication Token", SonarLintImages.IMG_WIZBAN_NEW_SERVER);
    this.model = model;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);

    GridLayout layout = new GridLayout();
    layout.numColumns = 3;
    container.setLayout(layout);

    GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
    container.setLayoutData(layoutData);

    createTokenField(container);
    createOpenSecurityPageButton(container);

    DataBindingContext dbc = new DataBindingContext();
    dbc.bindValue(
      WidgetProperties.text(SWT.Modify).observe(serverTokenText),
      BeanProperties.value(ServerConnectionModel.class, ServerConnectionModel.PROPERTY_USERNAME)
        .observe(model));

    WizardPageSupport.create(this, dbc);

    setControl(container);
  }

  private void createTokenField(final Composite container) {
    Label labelUsername = new Label(container, SWT.NULL);
    labelUsername.setText("Token:");
    serverTokenText = new Text(container, SWT.BORDER | SWT.SINGLE);
    serverTokenText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
  }

  private void createOpenSecurityPageButton(Composite container) {
    Button button = new Button(container, SWT.PUSH);
    button.setImage(SonarLintImages.IMG_OPEN_EXTERNAL);
    button.setText(Messages.ServerLocationWizardPage_action_token);
    button.setToolTipText(Messages.ServerLocationWizardPage_action_token_tooltip);
    button.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

    button.addListener(SWT.Selection, e -> openSecurityPage());
  }

  private void openSecurityPage() {
    StringBuilder url = new StringBuilder(256);
    url.append(model.getServerUrl());
    url.append("/account/security");
    try {
      PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(url.toString()));
    } catch (PartInitException | MalformedURLException e) {
      SonarLintLogger.get().error("Unable to open external browser", e);
      MessageDialog.openError(this.getShell(), "Error", "Unable to open external browser: " + e.getMessage());
    }
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    // Clear validation message when coming back to this page
    setMessage(null);
  }

}
