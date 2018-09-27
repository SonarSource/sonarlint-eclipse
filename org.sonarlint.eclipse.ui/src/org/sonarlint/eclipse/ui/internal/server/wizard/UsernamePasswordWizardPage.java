/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2018 SonarSource SA
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

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.wizard.WizardPageSupport;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.sonarlint.eclipse.ui.internal.Messages;

public class UsernamePasswordWizardPage extends AbstractServerConnectionWizardPage {

  private Text serverUsernameText;
  private Text serverPasswordText;

  private Binding usernameTextBinding;

  private Binding passwordTextBinding;

  public UsernamePasswordWizardPage(ServerConnectionModel model) {
    super("server_credentials_page", "SonarQube Server Credentials", model, 2);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void doCreateControl(Composite container) {
    createUsernameOrTokenField(container);
    createPasswordField(container);

    DataBindingContext dbc = new DataBindingContext();
    usernameTextBinding = dbc.bindValue(
      WidgetProperties.text(SWT.Modify).observe(serverUsernameText),
      BeanProperties.value(ServerConnectionModel.class, ServerConnectionModel.PROPERTY_USERNAME)
        .observe(model),
      new UpdateValueStrategy().setBeforeSetValidator(
        new MandatoryStringValidator("You must provide a login")),
      null);
    ControlDecorationSupport.create(usernameTextBinding, SWT.LEFT | SWT.TOP);
    passwordTextBinding = dbc.bindValue(
      WidgetProperties.text(SWT.Modify).observe(serverPasswordText),
      BeanProperties.value(ServerConnectionModel.class, ServerConnectionModel.PROPERTY_PASSWORD)
        .observe(model),
      new UpdateValueStrategy().setBeforeSetValidator(
        new MandatoryStringValidator("You must provide a password")),
      null);
    ControlDecorationSupport.create(passwordTextBinding, SWT.LEFT | SWT.TOP);

    WizardPageSupport.create(this, dbc);
  }

  private void createPasswordField(final Composite container) {
    Label labelPassword = new Label(container, SWT.NULL);
    labelPassword.setText(Messages.ServerLocationWizardPage_label_password);
    serverPasswordText = new Text(container, SWT.BORDER | SWT.SINGLE | SWT.PASSWORD);
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalIndent = 10;
    serverPasswordText.setLayoutData(gd);
  }

  private void createUsernameOrTokenField(final Composite container) {
    Label labelUsername = new Label(container, SWT.NULL);
    labelUsername.setText("Username:");
    serverUsernameText = new Text(container, SWT.BORDER | SWT.SINGLE);
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalIndent = 10;
    serverUsernameText.setLayoutData(gd);
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (visible) {
      usernameTextBinding.validateTargetToModel();
      passwordTextBinding.validateTargetToModel();
    }
  }

}
