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
package org.sonarlint.eclipse.ui.internal.popup;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectionFacade;
import org.sonarlint.eclipse.core.internal.telemetry.SonarLintTelemetry;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.EditNotificationsWizard;
import org.sonarlint.eclipse.ui.internal.util.BrowserUtils;
import org.sonarsource.sonarlint.core.rpc.protocol.client.smartnotification.ShowSmartNotificationParams;

public class DeveloperNotificationPopup extends AbstractSonarLintPopup {

  private final ShowSmartNotificationParams notification;
  private final boolean isSonarCloud;
  private final String sqOrSc;
  private final ConnectionFacade connection;

  public DeveloperNotificationPopup(ConnectionFacade connection, ShowSmartNotificationParams notification, boolean isSonarCloud) {
    this.connection = connection;
    this.notification = notification;
    this.isSonarCloud = isSonarCloud;
    sqOrSc = isSonarCloud ? "SonarQube Cloud" : "SonarQube Server";
  }

  @Override
  protected String getMessage() {
    return notification.getText();
  }

  @Override
  protected void createContentArea(Composite composite) {
    super.createContentArea(composite);

    addLink("Open in " + sqOrSc, e -> {
      SonarLintTelemetry.devNotificationsClicked(notification.getCategory());
      BrowserUtils.openExternalBrowser(notification.getLink(), e.display);
      close();
    });

    addLink("Configure", e -> {
      var wd = EditNotificationsWizard.createDialog(getParentShell(), connection);
      wd.open();
    });
  }

  @Override
  protected String getPopupShellTitle() {
    return sqOrSc + " Notification";
  }

  @Override
  protected Image getPopupShellImage(int maximumHeight) {
    return isSonarCloud ? SonarLintImages.SONARCLOUD_SERVER_ICON_IMG : SonarLintImages.SONARQUBE_SERVER_ICON_IMG;
  }
}
