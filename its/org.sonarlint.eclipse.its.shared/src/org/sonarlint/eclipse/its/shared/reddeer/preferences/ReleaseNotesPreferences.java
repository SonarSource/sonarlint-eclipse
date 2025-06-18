/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonarlint.eclipse.its.shared.reddeer.preferences;

import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.core.condition.WidgetIsFound;
import org.eclipse.reddeer.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.reddeer.swt.api.Browser;
import org.eclipse.reddeer.swt.condition.PageIsLoaded;
import org.eclipse.reddeer.swt.impl.browser.InternalBrowser;
import org.eclipse.reddeer.workbench.workbenchmenu.WorkbenchMenuPreferencesDialog;
import org.sonarlint.eclipse.its.shared.AbstractSonarLintTest;

public class ReleaseNotesPreferences extends PropertyPage {
  public ReleaseNotesPreferences(WorkbenchMenuPreferencesDialog preferenceDialog) {
    super(preferenceDialog, "SonarQube", "Release Notes");
  }

  public void cancel() {
    ((WorkbenchMenuPreferencesDialog) referencedComposite).cancel();
  }

  public Browser getFirstBrowser() {
    // Browser can take a while to render
    new WaitUntil(new WidgetIsFound(org.eclipse.swt.browser.Browser.class, getControl()),
      TimePeriod.DEFAULT, false);
    return new InternalBrowser(referencedComposite);
  }

  public String getFlatTextContent() {
    new WaitUntil(new PageIsLoaded(getFirstBrowser()));
    return getFirstBrowser().getText();
  }

  public static ReleaseNotesPreferences open() {
    var preferenceDialog = AbstractSonarLintTest.openPreferenceDialog();
    var releaseNotesPreferences = new ReleaseNotesPreferences(preferenceDialog);
    preferenceDialog.select(releaseNotesPreferences);
    return releaseNotesPreferences;
  }
}
