/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2020 SonarSource SA
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarsource.sonarlint.core.client.api.notifications.SonarQubeNotification;

public class DeveloperNotificationPopup extends AbstractSonarLintPopup {

  private final SonarQubeNotification notification;
  private final boolean isSonarCloud;

  public DeveloperNotificationPopup(Display display, SonarQubeNotification notification, boolean isSonarCloud) {
    super(display);
    this.notification = notification;
    this.isSonarCloud = isSonarCloud;
  }

  @Override
  protected String getMessage() {
    return notification.message();
  }

  @Override
  protected void createContentArea(Composite composite) {
    super.createContentArea(composite);

    addLink("Check it here", e -> {
      DeveloperNotificationPopup.this.close();
      try {
        PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(notification.link()));
      } catch (PartInitException | MalformedURLException e1) {
        // ignore
      }
    });
  }

  @Override
  protected String getPopupShellTitle() {
    return isSonarCloud ? "SonarCloud event" : "SonarQube event";
  }

  @Override
  protected Image getPopupShellImage(int maximumHeight) {
    return SonarLintImages.BALLOON_IMG;
  }
}
