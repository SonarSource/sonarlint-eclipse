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

import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarsource.sonarlint.core.commons.Language;

/***/
public class LanguageFromConnectedModePopup extends AbstractSonarLintPopup {
  private final List<Language> languages;
  
  public LanguageFromConnectedModePopup(List<Language> languages) {
    this.languages = languages;
  }
  
  @Override
  protected String getMessage() {
    if (languages.size() == 1) {
      return "You tried to analyze a " + languages.get(0).getLabel()
        + " file. This language analyzer is only available in connected mode.";
    }
    
    var languagesList = String.join(" / ", languages.stream().map(l -> l.getLabel()).collect(Collectors.toList()));
    return "You tried to analyze " + languagesList
      + " files. These language analyzers are only available in connected mode.";
  }

  @Override
  protected void createContentArea(Composite composite) {
    super.createContentArea(composite);
    
    addLink("Learn more", e -> {
      // TODO: Implement
    });
    
    addLink("Try SonarCloud for free", e -> {
      // TODO: Implement
    });
    
    addLink("Don't show again", e -> {
      SonarLintGlobalConfiguration.setIgnoreMissingFeatureNotifications();
      close();
    });
  }

  @Override
  protected String getPopupShellTitle() {
    return "SonarLint - Language" + (languages.size() > 1 ? "s" : "") + " could not be analyzed";
  }

  @Override
  protected Image getPopupShellImage(int maximumHeight) {
    return SonarLintImages.BALLOON_IMG;
  }
  
  /**  */
  public static void displayPopupIfNotIgnored(List<Language> languages) {
    if (languages.isEmpty() || SonarLintGlobalConfiguration.ignoreMissingFeatureNotifications() ) {
      return;
    }
    
    var popup = new LanguageFromConnectedModePopup(languages);
    popup.setFadingEnabled(false);
    popup.setDelayClose(0L);
    popup.open();
  }
}
