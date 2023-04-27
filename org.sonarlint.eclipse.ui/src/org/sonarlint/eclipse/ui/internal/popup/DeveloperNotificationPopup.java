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
package org.sonarlint.eclipse.ui.internal.popup;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.connected.IConnectedEngineFacade;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.EditNotificationsWizard;
import org.sonarlint.eclipse.ui.internal.util.BrowserUtils;
import org.sonarsource.sonarlint.core.clientapi.client.smartnotification.ShowSmartNotificationParams;

public class DeveloperNotificationPopup extends AbstractSonarLintPopup {

  private final ShowSmartNotificationParams notification;
  private final boolean isSonarCloud;
  private final String sqOrSc;
  private final IConnectedEngineFacade server;

  public DeveloperNotificationPopup(IConnectedEngineFacade server, ShowSmartNotificationParams notification, boolean isSonarCloud) {
    this.server = server;
    this.notification = notification;
    this.isSonarCloud = isSonarCloud;
    sqOrSc = isSonarCloud ? "SonarCloud" : "SonarQube";
  }

  @Override
  protected String getMessage() {
    return notification.getText();
  }

  @Override
  protected void createContentArea(Composite composite) {
    super.createContentArea(composite);

    addLink("Open in " + sqOrSc, e -> {
      var telemetry = SonarLintCorePlugin.getTelemetry();
      telemetry.devNotificationsClicked(notification.getCategory());
      BrowserUtils.openExternalBrowser(notification.getLink());
      close();
    });

    addLink("Configure", e -> {
      var wd = EditNotificationsWizard.createDialog(getParentShell(), server);
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
