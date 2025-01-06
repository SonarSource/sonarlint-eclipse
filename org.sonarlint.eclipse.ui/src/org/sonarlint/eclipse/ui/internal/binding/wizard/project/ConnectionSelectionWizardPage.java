/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.binding.wizard.project;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.wizard.WizardPageSupport;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectionFacade;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.ServerConnectionWizard;
import org.sonarlint.eclipse.ui.internal.util.wizard.BeanPropertiesCompat;
import org.sonarlint.eclipse.ui.internal.util.wizard.ViewersObservablesCompat;

public class ConnectionSelectionWizardPage extends AbstractProjectBindingWizardPage {

  private Binding connectionBinding;

  public ConnectionSelectionWizardPage(ProjectBindingModel model) {
    super("connection_select_page", "Choose the SonarQube (Server, Cloud) connection", model, 2);
  }

  @Override
  protected void doCreateControl(Composite container) {
    var connectionCombo = new ComboViewer(container, SWT.READ_ONLY);
    connectionCombo.setContentProvider(ArrayContentProvider.getInstance());
    connectionCombo.setLabelProvider(new LabelProvider() {
      @Override
      public String getText(Object element) {
        var current = (ConnectionFacade) element;
        return current.getId();
      }

      @Override
      public Image getImage(Object element) {
        if (((ConnectionFacade) element).isSonarCloud()) {
          return SonarLintImages.SONARCLOUD_SERVER_ICON_IMG;
        } else {
          return SonarLintImages.SONARQUBE_SERVER_ICON_IMG;
        }
      }
    });
    connectionCombo.setInput(SonarLintCorePlugin.getConnectionManager().getConnections());
    var connection = model.getConnection();
    if (connection != null) {
      final var selection = new StructuredSelection(connection);
      connectionCombo.setSelection(selection);
    }

    var dataBindingContext = new DataBindingContext();
    connectionBinding = dataBindingContext.bindValue(
      ViewersObservablesCompat.observeSingleSelection(connectionCombo),
      BeanPropertiesCompat.value(ProjectBindingModel.class, ProjectBindingModel.PROPERTY_CONNECTION)
        .observe(model),
      new UpdateValueStrategy().setBeforeSetValidator(new MandatoryConnectionValidator("You must select a connection")), null);
    ControlDecorationSupport.create(connectionBinding, SWT.LEFT | SWT.TOP);

    WizardPageSupport.create(this, dataBindingContext);

    var addBtn = new Button(container, SWT.PUSH);
    addBtn.setText("New...");
    addBtn.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        ServerConnectionWizard.createDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), model.getEclipseProjects()).open();
        getContainer().getShell().close();
      }

    });
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (visible) {
      connectionBinding.validateTargetToModel();
    }
  }

}
