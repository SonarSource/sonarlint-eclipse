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
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.properties.AboutPropertyPage;
import org.sonarlint.eclipse.ui.internal.util.BrowserUtils;

/**
 *  Pop-up shown to the user once a new survey is to be filled out, will not be shown when the user already clicked the
 *  survey link. Can be accessed once again from the "Miscellaneous" page in the SonarLint preferences.
 */
public class SurveyPopup extends AbstractSonarLintPopup {
  private final String surveyURL;

  public SurveyPopup(String surveyURL) {
    this.surveyURL = surveyURL;
  }

  @Override
  protected String getMessage() {
    return "We are seeking your feedback to improve SonarLint for Eclipse with a short and simple user survey.";
  }

  @Override
  protected void createContentArea(Composite composite) {
    super.createContentArea(composite);

    addLink("Check out in browser", e -> {
      SonarLintGlobalConfiguration.setUserSurveyLastLink(surveyURL);
      BrowserUtils.openExternalBrowser(surveyURL, getShell().getDisplay());
      close();
      PreferencesUtil.createPreferenceDialogOn(getParentShell(), AboutPropertyPage.ABOUT_CONFIGURATION_ID,
        new String[] {AboutPropertyPage.ABOUT_CONFIGURATION_ID}, null).open();
    });
  }

  @Override
  protected String getPopupShellTitle() {
    return "SonarLint - New Eclipse user survey";
  }

  @Override
  protected Image getPopupShellImage(int maximumHeight) {
    return SonarLintImages.BALLOON_IMG;
  }

  /**
   *  Will display the pop-up as long as the user hasn't checked out the survey. The link is stored in the preferences
   *  if the user wants to come back and for the next survey to check if the user has already attended it or not.
   *
   *  @param link to the survey
   */
  public static void displaySurveyPopupIfNotAlreadyAccessed(String link) {
    var preference = SonarLintGlobalConfiguration.getUserSurveyLastLink();
    if (link.equals(preference)) {
      return;
    }

    var popup = new SurveyPopup(link);
    popup.setFadingEnabled(false);
    popup.setDelayClose(0L);
    popup.open();
  }
}
