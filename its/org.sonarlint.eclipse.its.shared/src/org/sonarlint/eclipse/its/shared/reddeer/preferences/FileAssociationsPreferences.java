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

import org.eclipse.reddeer.common.exception.RedDeerException;
import org.eclipse.reddeer.core.matcher.WithLabelMatcher;
import org.eclipse.reddeer.core.reference.ReferencedComposite;
import org.eclipse.reddeer.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.reddeer.swt.impl.combo.DefaultCombo;
import org.eclipse.reddeer.workbench.workbenchmenu.WorkbenchMenuPreferencesDialog;
import org.sonarlint.eclipse.its.shared.AbstractSonarLintTest;

public class FileAssociationsPreferences extends PropertyPage {
  private static final String LABEL = "Open unassociated files with:";

  public FileAssociationsPreferences(ReferencedComposite referencedComposite) {
    super(referencedComposite, "General", "Editors", "File Associations");
  }

  public static FileAssociationsPreferences open() {
    var preferenceDialog = AbstractSonarLintTest.openPreferenceDialog();
    var preferences = new FileAssociationsPreferences(preferenceDialog);
    preferenceDialog.select(preferences);
    return preferences;
  }

  public void resetFileAssociation() {
    // With Eclipse 4.34 (2024-12) they changed the label inside the combo box from "Text Editor" to "Plain Text Editor"
    try {
      new DefaultCombo(this, new WithLabelMatcher(LABEL)).setSelection("System Editor; if none: Text Editor");
    } catch (RedDeerException ignored) {
      new DefaultCombo(this, new WithLabelMatcher(LABEL)).setSelection("System Editor; if none: Plain Text Editor");
    }
  }

  public void enforceFileAssociation() {
    // With Eclipse 4.34 (2024-12) they changed the label inside the combo box from "Text Editor" to "Plain Text Editor"
    try {
      new DefaultCombo(this, new WithLabelMatcher(LABEL)).setSelection("Text Editor");
    } catch (RedDeerException ignored) {
      new DefaultCombo(this, new WithLabelMatcher(LABEL)).setSelection("Plain Text Editor");
    }
  }

  public void ok() {
    ((WorkbenchMenuPreferencesDialog) referencedComposite).ok();
  }
}
