/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2021 SonarSource SA
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

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.wizard.WizardPageSupport;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.ServerConnectionModel.ConnectionType;
import org.sonarlint.eclipse.ui.internal.util.PlatformUtils;

public class NotificationsWizardPage extends WizardPage {

  private final ServerConnectionModel model;
  private Button notificationsEnabledCheckbox;
  private Link notificationsDetails;
  private Composite container;

  public NotificationsWizardPage(ServerConnectionModel model) {
    super("connection_notification_page", "Configure Notifications", SonarLintImages.IMG_WIZBAN_NEW_SERVER);
    this.model = model;
  }

  @Override
  public void createControl(Composite parent) {

    container = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    container.setLayout(layout);

    GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
    container.setLayoutData(layoutData);

    GridData gd = new GridData(GridData.FILL_HORIZONTAL);

    notificationsEnabledCheckbox = new Button(container, SWT.CHECK);
    notificationsEnabledCheckbox.setLayoutData(gd);

    DataBindingContext dbc = new DataBindingContext();
    dbc.bindValue(
      WidgetProperties.selection().observe(notificationsEnabledCheckbox),
      BeanProperties.value(ServerConnectionModel.class, ServerConnectionModel.PROPERTY_NOTIFICATIONS_ENABLED)
        .observe(model),
      null,
      null);

    WizardPageSupport.create(this, dbc);

    notificationsDetails = new Link(container, SWT.WRAP);
    notificationsDetails.setLayoutData(gd);
    notificationsDetails.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        try {
          PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(e.text));
        } catch (PartInitException | MalformedURLException ex) {
          SonarLintLogger.get().error("Unable to open the browser", ex);
        }
      }
    });

    setControl(container);
  }

  @Override
  public void setVisible(boolean visible) {
    if (visible) {
      final boolean isSc = model.getConnectionType() == ConnectionType.SONARCLOUD;
      final String sqOrSc = isSc ? "SonarCloud" : "SonarQube";
      notificationsEnabledCheckbox.setText("Receive notifications from " + sqOrSc);
      PlatformUtils.requestLayout(notificationsEnabledCheckbox);
      final String docUrl = isSc ? "https://sonarcloud.io/documentation/user-guide/sonarlint-notifications/"
        : "https://docs.sonarqube.org/latest/user-guide/sonarlint-notifications/";
      notificationsDetails.setText("You will receive <a href=\"" + docUrl + "\">notifications</a> from " + sqOrSc + " in situations like:\n" +
        "  - the Quality Gate status of a bound project changes\n" +
        "  - the latest analysis of a bound project on " + sqOrSc + " raises new issues assigned to you");
      PlatformUtils.requestLayout(notificationsDetails);
    }
    super.setVisible(visible);
  }

}
