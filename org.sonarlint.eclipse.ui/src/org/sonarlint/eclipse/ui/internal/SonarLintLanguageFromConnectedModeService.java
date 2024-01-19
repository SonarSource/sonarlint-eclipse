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
package org.sonarlint.eclipse.ui.internal;

import java.util.ArrayList;
import org.sonarlint.eclipse.core.internal.event.AnalysisEvent;
import org.sonarlint.eclipse.core.internal.event.AnalysisListener;
import org.sonarlint.eclipse.ui.internal.popup.LanguageFromConnectedModePopup;

/** Service to handle languages from connected mode after analysis -> applies to standalone projects only */
public class SonarLintLanguageFromConnectedModeService implements AnalysisListener {
  @Override
  public void usedAnalysis(AnalysisEvent event) {
    LanguageFromConnectedModePopup.displayPopupIfNotIgnored(new ArrayList<>(event.getProjects()),
      new ArrayList<>(event.getUnavailableLanguages()));
  }
}
