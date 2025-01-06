/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.dialog;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.sonarlint.eclipse.core.analysis.SonarLintLanguage;

/** Version of the dialog where the fix suggestion is available! */
public class FixSuggestionAvailableDialog extends AbstractFixSuggestionDialog {
  public FixSuggestionAvailableDialog(Shell parentShell, @Nullable SonarLintLanguage language, String explanation,
    String textLeft, String textRight, int snippetIndex, int absoluteNumberOfChanges) {
    super(parentShell, language, explanation, textLeft, textRight, snippetIndex, absoluteNumberOfChanges);
  }

  /**
   *  "Apply the change"   -> applies the change and moves on to the next one
   *  "Decline the change" -> does not apply the change, but moves on to the next one
   *  "Cancel"             -> cancels the whole process, nothing will be further applied
   *
   *  The order is based on how it will be displayed in dialog. The first one is always the most right one (because of
   *  the "true") and the others are aligned to its left from left to right!
   */
  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, "Apply the change", true);
    createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", false);
    createButton(parent, IDialogConstants.SKIP_ID, "Decline the change", false);
  }
}
