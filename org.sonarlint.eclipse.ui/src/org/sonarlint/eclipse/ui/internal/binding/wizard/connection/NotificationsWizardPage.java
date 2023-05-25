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

import org.eclipse.core.databinding.DataBindingContext;
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
import org.eclipse.swt.widgets.Text;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.ServerConnectionModel.ConnectionType;
import org.sonarlint.eclipse.ui.internal.util.BrowserUtils;
import org.sonarlint.eclipse.ui.internal.util.wizard.BeanPropertiesCompat;
import org.sonarlint.eclipse.ui.internal.util.wizard.WidgetPropertiesCompat;

public class NotificationsWizardPage extends WizardPage {

  private final ServerConnectionModel model;
  private Button notificationsEnabledCheckbox;
  private Link notificationsLink;
  private Text notificationsDetails;
  private Composite container;

  public NotificationsWizardPage(ServerConnectionModel model) {
    super("connection_notification_page", "Configure Notifications", SonarLintImages.IMG_WIZBAN_NEW_SERVER);
    this.model = model;
  }

  @Override
  public void createControl(Composite parent) {

    container = new Composite(parent, SWT.NONE);
    var layout = new GridLayout();
    container.setLayout(layout);

    var layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
    container.setLayoutData(layoutData);

    var gd = new GridData(GridData.FILL_HORIZONTAL);

    notificationsEnabledCheckbox = new Button(container, SWT.CHECK);
    notificationsEnabledCheckbox.setLayoutData(gd);

    var dataBindingContext = new DataBindingContext();
    dataBindingContext.bindValue(
      WidgetPropertiesCompat.buttonSelection().observe(notificationsEnabledCheckbox),
      BeanPropertiesCompat.value(ServerConnectionModel.class, ServerConnectionModel.PROPERTY_NOTIFICATIONS_ENABLED)
        .observe(model),
      null,
      null);

    WizardPageSupport.create(this, dataBindingContext);

    notificationsLink = new Link(container, SWT.NONE);
    notificationsLink.setLayoutData(gd);
    notificationsLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        BrowserUtils.openExternalBrowser(e.text);
      }
    });

    notificationsDetails = new Text(container, SWT.WRAP);
    notificationsLink.setLayoutData(gd);

    setControl(container);
  }

  @Override
  public void setVisible(boolean visible) {
    if (visible) {
      final var isSc = model.getConnectionType() == ConnectionType.SONARCLOUD;
      final var sqOrSc = isSc ? "SonarCloud" : "SonarQube";
      notificationsEnabledCheckbox.setText("Receive notifications from " + sqOrSc);
      final var docUrl = isSc ? "https://docs.sonarcloud.io/advanced-setup/sonarlint-smart-notifications/"
        : "https://docs.sonarqube.org/latest/user-guide/sonarlint-connected-mode/#smart-notifications";
      notificationsLink.setText("You will receive <a href=\"" + docUrl + "\">notifications</a> from " + sqOrSc + " in situations like:");
      notificationsDetails.setText(
        "  - the Quality Gate status of a bound project changes\n" +
          "  - the latest analysis of a bound project on " + sqOrSc + " raises new issues assigned to you");
      container.requestLayout();
    }
    super.setVisible(visible);
  }

}
