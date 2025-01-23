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
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.sonarlint.eclipse.ui.internal.properties.ReleaseNotesPage;
import org.sonarlint.eclipse.ui.internal.util.PopupUtils;

/**
 *  Used to raise awareness about recent changes made in SonarLint for Eclipse. This will help users discover new
 *  features and what has changed in general.
 */
public class ReleaseNotesPopup extends AbstractSonarLintVersionPopup {
  public ReleaseNotesPopup() {
    super("SonarQube for Eclipse - Release Notes",
      "Thank you for installing / updating SonarQube for Eclipse (formerly known as SonarLint). We invite you to "
        + "learn about the recent changes by taking a look at the Release Notes. If you want to read them later, they "
        + "can be found nested into the SonarQube preferences.");
  }

  @Override
  protected void addLinks() {
    addLink("Show Release Notes", e -> {
      PreferencesUtil.createPreferenceDialogOn(getParentShell(), ReleaseNotesPage.ABOUT_CONFIGURATION_ID,
        null, null).open();
      close();
    });
  }

  /** This way everyone calling the pop-up does not have to handle it being actually displayed or not */
  public static void displayPopupIfNotAlreadyShown() {
    if (PopupUtils.popupCurrentlyDisplayed(ReleaseNotesPopup.class)) {
      return;
    }

    Display.getDefault().asyncExec(() -> {
      PopupUtils.addCurrentlyDisplayedPopup(ReleaseNotesPopup.class);

      var popup = new ReleaseNotesPopup();
      popup.setFadingEnabled(false);
      popup.setDelayClose(0L);
      popup.open();
    });
  }
}
