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
package org.sonarlint.eclipse.ui.internal.popup;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.connected.IConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.telemetry.SonarLintTelemetry;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.EditNotificationsWizard;
import org.sonarsource.sonarlint.core.client.api.notifications.ServerNotification;

public class DeveloperNotificationPopup extends AbstractSonarLintPopup {

  private final ServerNotification notification;
  private final boolean isSonarCloud;
  private final String sqOrSc;
  private final IConnectedEngineFacade server;

  public DeveloperNotificationPopup(IConnectedEngineFacade server, ServerNotification notification, boolean isSonarCloud) {
    this.server = server;
    this.notification = notification;
    this.isSonarCloud = isSonarCloud;
    sqOrSc = isSonarCloud ? "SonarCloud" : "SonarQube";
  }

  @Override
  protected String getMessage() {
    return notification.message();
  }

  @Override
  protected void createContentArea(Composite composite) {
    super.createContentArea(composite);

    addLink("Open in " + sqOrSc, e -> {
      SonarLintTelemetry telemetry = SonarLintCorePlugin.getTelemetry();
      telemetry.devNotificationsClicked(notification.category());
      try {
        PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(notification.link()));
      } catch (PartInitException | MalformedURLException e1) {
        // ignore
      }
      close();
    });

    addLink("Configure", e -> {
      WizardDialog wd = EditNotificationsWizard.createDialog(getParentShell(), server);
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
