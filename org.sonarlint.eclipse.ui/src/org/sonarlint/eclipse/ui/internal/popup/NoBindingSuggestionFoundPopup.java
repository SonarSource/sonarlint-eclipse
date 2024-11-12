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
import org.eclipse.swt.widgets.Display;
import org.sonarlint.eclipse.core.documentation.SonarLintDocumentation;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.util.BrowserUtils;
import org.sonarlint.eclipse.ui.internal.util.PopupUtils;

/**
 *  Pop-up shown to users when we try to bind the project automatically and it fails as no valid project can be
 *  matched. This can be due no project opened at all or all current ones just cannot match.
 */
public class NoBindingSuggestionFoundPopup extends AbstractSonarLintPopup {
  private final String configurationScopeId;
  private final boolean isSonarCloud;

  public NoBindingSuggestionFoundPopup(String configurationScopeId, boolean isSonarCloud) {
    this.configurationScopeId = configurationScopeId;
    this.isSonarCloud = isSonarCloud;
  }

  @Override
  protected String getMessage() {
    return "The SonarQube "
      + (isSonarCloud ? "Cloud" : "Server")
      + " project '" + configurationScopeId + "' cannot be matched to any project in the workspace. "
      + "Please open your project, or bind it manually, and try again.";
  }

  @Override
  protected void createContentArea(Composite composite) {
    super.createContentArea(composite);

    addLink("Open Troubleshooting documentation", e -> {
      BrowserUtils.openExternalBrowser(SonarLintDocumentation.TROUBLESHOOTING_LINK, getShell().getDisplay());
      close();
    });

    composite.getShell().addDisposeListener(e -> PopupUtils.removeCurrentlyDisplayedPopup(getClass()));
  }

  @Override
  protected String getPopupShellTitle() {
    return "SonarQube " + (isSonarCloud ? "Cloud" : "Server") + " - No matching open project found";
  }

  @Override
  protected Image getPopupShellImage(int maximumHeight) {
    return isSonarCloud
      ? SonarLintImages.SONARCLOUD_SERVER_ICON_IMG
      : SonarLintImages.SONARQUBE_SERVER_ICON_IMG;
  }

  /** This way everyone calling the pop-up does not have to handle it being actually displayed or not */
  public static void displayPopupIfNotIgnored(String configurationScopeId, boolean isSonarCloud) {
    if (PopupUtils.popupCurrentlyDisplayed(NoBindingSuggestionFoundPopup.class)) {
      return;
    }

    Display.getDefault().asyncExec(() -> {
      PopupUtils.addCurrentlyDisplayedPopup(NoBindingSuggestionFoundPopup.class);

      var popup = new NoBindingSuggestionFoundPopup(configurationScopeId, isSonarCloud);
      popup.setFadingEnabled(false);
      popup.setDelayClose(0L);
      popup.open();
    });
  }
}
