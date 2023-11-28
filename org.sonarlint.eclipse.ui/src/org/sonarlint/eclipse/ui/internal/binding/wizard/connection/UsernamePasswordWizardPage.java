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

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.wizard.WizardPageSupport;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.sonarlint.eclipse.ui.internal.Messages;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.util.wizard.BeanPropertiesCompat;
import org.sonarlint.eclipse.ui.internal.util.wizard.WidgetPropertiesCompat;

/**
 *  @deprecated SonarCloud only offers authentication via token, SonarQube should follow soon
 */
@Deprecated(since="9.1", forRemoval=true)
public class UsernamePasswordWizardPage extends AbstractServerConnectionWizardPage {
  public static final String DEPRECATION_MESSAGE = "Authentication via username and password is deprecated and will "
    + "be removed in the future. Please use a token instead.";
  
  private Text serverUsernameText;
  private Text serverPasswordText;

  private Binding usernameTextBinding;

  private Binding passwordTextBinding;

  public UsernamePasswordWizardPage(ServerConnectionModel model) {
    super("server_credentials_page", "SonarQube User Credentials", model, 2);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void doCreateControl(Composite container) {
    createDeprecationLabel(container);
    createUsernameOrTokenField(container);
    createPasswordField(container);

    var dataBindingContext = new DataBindingContext();
    usernameTextBinding = dataBindingContext.bindValue(
      WidgetPropertiesCompat.text(SWT.Modify).observe(serverUsernameText),
      BeanPropertiesCompat.value(ServerConnectionModel.class, ServerConnectionModel.PROPERTY_USERNAME)
        .observe(model),
      new UpdateValueStrategy().setBeforeSetValidator(
        new MandatoryStringValidator("You must provide a login")),
      null);
    ControlDecorationSupport.create(usernameTextBinding, SWT.LEFT | SWT.TOP);
    passwordTextBinding = dataBindingContext.bindValue(
      WidgetPropertiesCompat.text(SWT.Modify).observe(serverPasswordText),
      BeanPropertiesCompat.value(ServerConnectionModel.class, ServerConnectionModel.PROPERTY_PASSWORD)
        .observe(model),
      new UpdateValueStrategy().setBeforeSetValidator(
        new MandatoryStringValidator("You must provide a password")),
      null);
    ControlDecorationSupport.create(passwordTextBinding, SWT.LEFT | SWT.TOP);

    WizardPageSupport.create(this, dataBindingContext);
  }
  
  private static void createDeprecationLabel(final Composite container) {
    var deprecationContainer = new Composite(container, SWT.NONE);
    deprecationContainer.setLayout(new GridLayout(3, false));
    deprecationContainer.setLayoutData(new GridData(SWT.LEFT, SWT.DOWN, true, false, Integer.MAX_VALUE, 1));
    
    // icon on the left
    new Label(deprecationContainer, SWT.NULL).setImage(SonarLintImages.IMG_SEVERITY_BLOCKER);
    
    var labelDeprecation = new Label(deprecationContainer, SWT.NULL);
    labelDeprecation.setText(DEPRECATION_MESSAGE);
    labelDeprecation.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    labelDeprecation.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
    
    // icon on the right
    new Label(deprecationContainer, SWT.NULL).setImage(SonarLintImages.IMG_SEVERITY_BLOCKER);
  }

  private void createPasswordField(final Composite container) {
    var labelPassword = new Label(container, SWT.NULL);
    labelPassword.setText(Messages.ServerLocationWizardPage_label_password);
    serverPasswordText = new Text(container, SWT.BORDER | SWT.SINGLE | SWT.PASSWORD);
    var gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalIndent = 10;
    serverPasswordText.setLayoutData(gd);
  }

  private void createUsernameOrTokenField(final Composite container) {
    var labelUsername = new Label(container, SWT.NULL);
    labelUsername.setText("Username:");
    serverUsernameText = new Text(container, SWT.BORDER | SWT.SINGLE);
    var gd = new GridData(GridData.FILL_HORIZONTAL);
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
