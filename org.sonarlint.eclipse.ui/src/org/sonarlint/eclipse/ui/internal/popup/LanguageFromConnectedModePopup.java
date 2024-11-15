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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.sonarlint.eclipse.core.analysis.SonarLintLanguage;
import org.sonarlint.eclipse.core.documentation.SonarLintDocumentation;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.binding.wizard.project.ProjectBindingWizard;
import org.sonarlint.eclipse.ui.internal.util.BrowserUtils;
import org.sonarlint.eclipse.ui.internal.util.PopupUtils;
import org.sonarsource.sonarlint.core.client.utils.Language;

/**
 *  Pop-up shown to users analyzing (multiple) files in standalone mode while one (or more) are of languages that are
 *  only available in connected mode.
 */
public class LanguageFromConnectedModePopup extends AbstractSonarLintPopup {
  private final ISonarLintProject project;
  private final List<SonarLintLanguage> languages;

  public LanguageFromConnectedModePopup(ISonarLintProject project, List<SonarLintLanguage> languages) {
    this.project = project;
    this.languages = languages;
  }

  @Override
  protected String getMessage() {
    if (languages.size() == 1) {
      return "You tried to analyze a " + getLabel(languages.get(0))
        + " file. This language analysis is only available in Connected Mode.";
    }

    var languagesList = String.join(" / ", languages.stream().map(LanguageFromConnectedModePopup::getLabel).collect(Collectors.toList()));
    return "You tried to analyze " + languagesList
      + " files. These language analyses are only available in Connected Mode.";
  }

  private static String getLabel(SonarLintLanguage language) {
    return Language.valueOf(language.name()).getLabel();
  }

  @Override
  protected void createContentArea(Composite composite) {
    super.createContentArea(composite);

    addLink("Learn more",
      e -> BrowserUtils.openExternalBrowser(SonarLintDocumentation.RULES, getShell().getDisplay()));

    if (SonarLintCorePlugin.getConnectionManager().checkForSonarCloud()) {
      addLink("Bind to SonarQube Cloud", e -> ProjectBindingWizard.createDialog(getParentShell(), Set.of(project)));
    } else {
      addLink("Try SonarQube Cloud for free",
        e -> BrowserUtils.openExternalBrowser(SonarLintDocumentation.SONARCLOUD_SIGNUP_LINK, getShell().getDisplay()));
    }

    addLink("Don't show again", e -> {
      SonarLintGlobalConfiguration.setIgnoreMissingFeatureNotifications();
      close();
    });

    composite.getShell().addDisposeListener(e -> PopupUtils.removeCurrentlyDisplayedPopup(getClass()));
  }

  @Override
  protected String getPopupShellTitle() {
    return "SonarQube for Eclipse - Language" + (languages.size() > 1 ? "s" : "") + " could not be analyzed";
  }

  @Override
  protected Image getPopupShellImage(int maximumHeight) {
    return SonarLintImages.BALLOON_IMG;
  }

  /** This way everyone calling the pop-up does not have to handle it being actually displayed or not */
  public static void displayPopupIfNotIgnored(ISonarLintProject project, List<SonarLintLanguage> languages) {
    if (languages.isEmpty() || PopupUtils.popupCurrentlyDisplayed(LanguageFromConnectedModePopup.class)
      || SonarLintGlobalConfiguration.ignoreMissingFeatureNotifications()) {
      return;
    }

    Display.getDefault().asyncExec(() -> {
      PopupUtils.addCurrentlyDisplayedPopup(LanguageFromConnectedModePopup.class);

      var popup = new LanguageFromConnectedModePopup(project, languages);
      popup.setFadingEnabled(false);
      popup.setDelayClose(0L);
      popup.open();
    });
  }
}
