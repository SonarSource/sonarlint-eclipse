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

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.wizard.WizardPageSupport;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.sonarlint.eclipse.ui.internal.Messages;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.ServerConnectionModel.ConnectionType;
import org.sonarlint.eclipse.ui.internal.util.wizard.BeanPropertiesCompat;
import org.sonarlint.eclipse.ui.internal.util.wizard.WidgetPropertiesCompat;

public class ConnectionIdWizardPage extends AbstractServerConnectionWizardPage {

  private Binding connectionIdTextBinding;

  public ConnectionIdWizardPage(ServerConnectionModel model) {
    super("connection_id_page", null, model, 2);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void doCreateControl(Composite container) {
    var labelId = new Label(container, SWT.NULL);
    labelId.setText(Messages.ServerLocationWizardPage_label_id);
    var connectionIdText = new Text(container, SWT.BORDER | SWT.SINGLE);
    var gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalIndent = 10;
    connectionIdText.setLayoutData(gd);

    var dataBindingContext = new DataBindingContext();
    connectionIdTextBinding = dataBindingContext.bindValue(
      WidgetPropertiesCompat.text(SWT.Modify).observe(connectionIdText),
      BeanPropertiesCompat.value(ServerConnectionModel.class, ServerConnectionModel.PROPERTY_CONNECTION_ID)
        .observe(model),
      new UpdateValueStrategy().setBeforeSetValidator(
        new MandatoryAndUniqueServerIdValidator(model.isEdit())),
      null);
    ControlDecorationSupport.create(connectionIdTextBinding, SWT.LEFT | SWT.TOP);

    WizardPageSupport.create(this, dataBindingContext);
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (visible) {
      setTitle((model.getConnectionType() == ConnectionType.SONARCLOUD ? "SonarQube Cloud" : "SonarQube Server")
        + " Connection Identifier");
      connectionIdTextBinding.validateTargetToModel();
    }
  }

}
