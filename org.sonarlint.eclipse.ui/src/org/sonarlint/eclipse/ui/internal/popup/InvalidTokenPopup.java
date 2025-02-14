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
package org.sonarlint.eclipse.ui.internal.popup;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectionFacade;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.ServerConnectionWizard;
import org.sonarlint.eclipse.ui.internal.util.PopupUtils;

/**
 *  Pop-up shown and triggered by SonarLint CORE when a token used for a connection (both SonarQube Server and Cloud)
 *  is invalid. With this we provide the user a call to action to change the token easily by editing the connection
 *  which will open the connection wizard.
 */
public class InvalidTokenPopup extends AbstractSonarLintPopup {
  private final ConnectionFacade facade;

  public InvalidTokenPopup(ConnectionFacade facade) {
    this.facade = facade;
  }

  @Override
  protected String getMessage() {
    return "The token for the connection '" + facade.getId() + "' (" + facade.getHost() + ") is invalid. Please "
      + "change it to continue working in Connected Mode.";
  }

  @Override
  protected void createContentArea(Composite composite) {
    super.createContentArea(composite);

    addLink("Edit Connection", e -> {
      // INFO: We need to get the parent shell here as we act on the shell of the notification that closes afterwards!
      ServerConnectionWizard.createDialog(getParentShell(), facade).open();
      close();
    });
  }

  @Override
  protected String getPopupShellTitle() {
    var message = facade.isSonarCloud() ? "SonarQube Cloud" : "SonarQube Server";
    return message + " - Invalid token for connection";
  }

  @Override
  protected Image getPopupShellImage(int maximumHeight) {
    return facade.isSonarCloud()
      ? SonarLintImages.SONARCLOUD_SERVER_ICON_IMG
      : SonarLintImages.SONARQUBE_SERVER_ICON_IMG;
  }

  /** This way everyone calling the pop-up does not have to handle it being actually displayed or not */
  public static void displayPopupIfNotIgnored(ConnectionFacade facade) {
    if (PopupUtils.popupCurrentlyDisplayed(InvalidTokenPopup.class)) {
      return;
    }

    Display.getDefault().asyncExec(() -> {
      PopupUtils.addCurrentlyDisplayedPopup(InvalidTokenPopup.class);

      var popup = new InvalidTokenPopup(facade);
      popup.setFadingEnabled(false);
      popup.setDelayClose(0L);
      popup.open();
    });
  }
}
