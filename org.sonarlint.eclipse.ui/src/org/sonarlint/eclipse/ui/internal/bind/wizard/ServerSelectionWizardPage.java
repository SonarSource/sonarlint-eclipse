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
package org.sonarlint.eclipse.ui.internal.bind.wizard;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.databinding.wizard.WizardPageSupport;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;

public class ServerSelectionWizardPage extends AbstractProjectBindingWizardPage {

  private Binding serverBinding;

  public ServerSelectionWizardPage(ProjectBindingModel model) {
    super("server_id_page", "Choose the SonarQube or SonarCloud server connection", model, 1);
  }

  @Override
  protected void doCreateControl(Composite container) {
    ComboViewer serverCombo = new ComboViewer(container, SWT.READ_ONLY);
    serverCombo.setContentProvider(ArrayContentProvider.getInstance());
    serverCombo.setLabelProvider(new LabelProvider() {
      @Override
      public String getText(Object element) {
        IServer current = (IServer) element;
        return current.getId();
      }

      @Override
      public Image getImage(Object element) {
        if (((IServer) element).isSonarCloud()) {
          return SonarLintImages.SONARCLOUD_SERVER_ICON_IMG;
        } else {
          return SonarLintImages.SONARQUBE_SERVER_ICON_IMG;
        }
      }
    });
    serverCombo.setInput(SonarLintCorePlugin.getServersManager().getServers());

    DataBindingContext dbc = new DataBindingContext();
    serverBinding = dbc.bindValue(
      ViewersObservables.observeSingleSelection(serverCombo),
      BeanProperties.value(ProjectBindingModel.class, ProjectBindingModel.PROPERTY_SERVER)
        .observe(model),
      new UpdateValueStrategy().setBeforeSetValidator(new MandatoryServerValidator("You must select a server connection")), null);
    ControlDecorationSupport.create(serverBinding, SWT.LEFT | SWT.TOP);

    WizardPageSupport.create(this, dbc);
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (visible) {
      serverBinding.validateTargetToModel();
    }
  }

}
