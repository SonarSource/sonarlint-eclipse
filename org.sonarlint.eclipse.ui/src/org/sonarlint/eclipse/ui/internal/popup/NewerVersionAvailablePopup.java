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

import org.eclipse.swt.widgets.Display;
import org.sonarlint.eclipse.core.documentation.SonarLintDocumentation;
import org.sonarlint.eclipse.ui.internal.job.TriggerUpdateAction;
import org.sonarlint.eclipse.ui.internal.util.BrowserUtils;
import org.sonarlint.eclipse.ui.internal.util.PopupUtils;

/**
 *  Shown when a newer version is available. This won't take into account dogfooding versions!
 */
public class NewerVersionAvailablePopup extends AbstractSonarLintVersionPopup {
  public NewerVersionAvailablePopup(String version) {
    super("SonarQube for Eclipse - New version available",
      "A newer version of SonarQube for Eclipse has been released: " + version + ". Feel free to check it out or "
        + "trigger an update. In case no update is available in your IDE, SonarLint might have been installed "
        + "manually or is managed by your organization.");
  }

  @Override
  protected void addLinks() {
    addLink("Check out in browser", e -> {
      BrowserUtils.openExternalBrowser(SonarLintDocumentation.COMMUNITY_FORUM_ECLIPSE_RELEASES, getShell().getDisplay());
      BrowserUtils.openExternalBrowser(SonarLintDocumentation.GITHUB_RELEASES, getShell().getDisplay());
    });
    addLink("Check for updates", e -> {
      new TriggerUpdateAction().schedule();
      close();
    });
  }

  /** This way everyone calling the pop-up does not have to handle it being actually displayed or not */
  public static void displayPopupIfNotAlreadyShown(String version) {
    if (PopupUtils.popupCurrentlyDisplayed(NewerVersionAvailablePopup.class)) {
      return;
    }

    Display.getDefault().asyncExec(() -> {
      PopupUtils.addCurrentlyDisplayedPopup(NewerVersionAvailablePopup.class);

      var popup = new NewerVersionAvailablePopup(version);
      popup.setFadingEnabled(false);
      popup.setDelayClose(0L);
      popup.open();
    });
  }
}
