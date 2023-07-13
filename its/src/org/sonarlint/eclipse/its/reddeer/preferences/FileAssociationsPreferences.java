/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2023 SonarSource SA
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

import org.eclipse.reddeer.core.matcher.WithLabelMatcher;
import org.eclipse.reddeer.core.reference.ReferencedComposite;
import org.eclipse.reddeer.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.reddeer.swt.impl.combo.DefaultCombo;
import org.eclipse.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;

public class FileAssociationsPreferences extends PropertyPage {
  private static final String LABEL = "Open unassociated files with:";

  public FileAssociationsPreferences(ReferencedComposite referencedComposite) {
    super(referencedComposite, "General", "Editors", "File Associations");
  }

  public static FileAssociationsPreferences open() {
    var preferenceDialog = new WorkbenchPreferenceDialog();
    if (!preferenceDialog.isOpen()) {
      preferenceDialog.open();
    }

    var preferences = new FileAssociationsPreferences(preferenceDialog);
    preferenceDialog.select(preferences);
    return preferences;
  }

  public void resetFileAssociation() {
    new DefaultCombo(this, new WithLabelMatcher(LABEL)).setSelection("System Editor; if none: Text Editor");
  }

  public void enforceFileAssociation() {
    new DefaultCombo(this, new WithLabelMatcher(LABEL)).setSelection("Text Editor");
  }

  public void ok() {
    ((WorkbenchPreferenceDialog) referencedComposite).ok();
  }
}
