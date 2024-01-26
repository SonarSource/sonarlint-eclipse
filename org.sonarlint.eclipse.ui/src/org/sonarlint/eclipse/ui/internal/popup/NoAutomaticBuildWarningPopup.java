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

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.util.PopupUtils;

/**
 *  Pop-up shown to users when the automatic workspace build is disabled. This might impact the accuracy of the
 *  analysis as some rules require the context of the compiled bytecode.
 */
public class NoAutomaticBuildWarningPopup extends AbstractSonarLintPopup {
  @Override
  protected String getPopupShellTitle() {
    return "Automatic build of workspace disabled";
  }

  @Override
  protected String getMessage() {
    return "The accuracy of analysis results might be slightly impacted as some rules require the context of the "
      + "compiled bytecode provided by the automatic build of workspace.";
  }

  @Override
  protected Image getPopupShellImage(int maximumHeight) {
    return SonarLintImages.BALLOON_IMG;
  }

  @Override
  protected void createContentArea(Composite composite) {
    super.createContentArea(composite);

    addLink("Enable automatic build of workspace", e -> {
      PreferencesUtil.createPreferenceDialogOn(getParentShell(),
        "org.eclipse.ui.preferencePages.BuildOrder",
        null, null).open();
      close();
    });

    addLink("Don't show again", e -> {
      SonarLintGlobalConfiguration.setNoAutomaticBuildWarning();
      close();
    });

    composite.getShell().addDisposeListener(e -> PopupUtils.removeCurrentlyDisplayedPopup(getClass()));
  }

  /** This way everyone calling the pop-up does not have to handle it being actually displayed or not */
  public static void displayPopupIfNotIgnored() {
    if (ResourcesPlugin.getWorkspace().getDescription().isAutoBuilding()
      || PopupUtils.popupCurrentlyDisplayed(NoAutomaticBuildWarningPopup.class)
      || SonarLintGlobalConfiguration.noAutomaticBuildWarning() ) {
      return;
    }

    Display.getDefault().asyncExec(() -> {
      PopupUtils.addCurrentlyDisplayedPopup(NoAutomaticBuildWarningPopup.class);

      var popup = new NoAutomaticBuildWarningPopup();
      popup.setFadingEnabled(false);
      popup.setDelayClose(0L);
      popup.open();
    });
  }
}
