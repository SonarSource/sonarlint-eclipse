/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonarlint.eclipse.its.reddeer.preferences;

import org.eclipse.reddeer.core.matcher.WithTextMatcher;
import org.eclipse.reddeer.core.reference.ReferencedComposite;
import org.eclipse.reddeer.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.reddeer.swt.impl.button.CheckBox;
import org.eclipse.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;

public class GeneralWorkspaceBuildPreferences extends PropertyPage {
  public GeneralWorkspaceBuildPreferences(ReferencedComposite referencedComposite) {
    super(referencedComposite, "General", "Workspace", "Build");
  }

  public void disableAutomaticBuild() {
    var checkBox = new CheckBox(this, new WithTextMatcher("&Build automatically"));
    if (checkBox.isChecked()) {
      checkBox.click();
    }
  }

  public void enableAutomaticBuild() {
    var checkBox = new CheckBox(this, new WithTextMatcher("&Build automatically"));
    if (!checkBox.isChecked()) {
      checkBox.click();
    }
  }

  public void ok() {
    ((WorkbenchPreferenceDialog) referencedComposite).ok();
  }

  public static GeneralWorkspaceBuildPreferences open() {
    var preferenceDialog = new WorkbenchPreferenceDialog();
    if (!preferenceDialog.isOpen()) {
      preferenceDialog.open();
    }

    var generalWorkspaceBuildPreferences = new GeneralWorkspaceBuildPreferences(preferenceDialog);
    preferenceDialog.select(generalWorkspaceBuildPreferences);
    return generalWorkspaceBuildPreferences;
  }
}
