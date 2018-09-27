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

public class UrlWizardPage extends AbstractServerConnectionWizardPage {

  private Binding serverUrlTextBinding;

  public UrlWizardPage(ServerConnectionModel model) {
    super("server_url_page", "SonarQube Server URL", model, 2);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void doCreateControl(Composite container) {

    Label labelUrl = new Label(container, SWT.NULL);
    labelUrl.setText(Messages.ServerLocationWizardPage_label_host);
    Text serverUrlText = new Text(container, SWT.BORDER | SWT.SINGLE);
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalIndent = 10;
    serverUrlText.setLayoutData(gd);
    serverUrlText.setMessage("Example: https://sonarqube.mycompany.com");

    DataBindingContext dbc = new DataBindingContext();
    serverUrlTextBinding = dbc.bindValue(
      WidgetProperties.text(SWT.Modify).observe(serverUrlText),
      BeanProperties.value(ServerConnectionModel.class, ServerConnectionModel.PROPERTY_SERVER_URL)
        .observe(model),
      new UpdateValueStrategy().setBeforeSetValidator(new MandatoryURLValidator()),
      null);
    ControlDecorationSupport.create(serverUrlTextBinding, SWT.LEFT | SWT.TOP);

    WizardPageSupport.create(this, dbc);
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (visible) {
      serverUrlTextBinding.validateTargetToModel();
      getContainer().updateMessage();
    }
  }

}
