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
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.sonarlint.eclipse.core.analysis.SonarLintLanguage;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;

/** Version of the dialog where the fix suggestion is not available due to it not being found! */
public class FixSuggestionUnavailableDialog extends AbstractFixSuggestionDialog {
  public FixSuggestionUnavailableDialog(Shell parentShell, @Nullable SonarLintLanguage language, String explanation,
    String textLeft, String textRight, int snippetIndex, int absoluteNumberOfChanges) {
    super(parentShell, language, explanation, textLeft, textRight, snippetIndex, absoluteNumberOfChanges);
  }

  /**
   *  "Proceed" -> does not apply the change and moves on to the next one
   *  "Cancel"  -> cancels the whole process, nothing will be further applied
   *
   *  The order is based on how it will be displayed in dialog. The first one is always the most right one (because of
   *  the "true") and the others are aligned to its left from left to right!
   */
  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, "Proceed", true);
    createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", false);
  }

  @Override
  protected void addLabel(Composite container) {
    var label = new CLabel(container, SWT.WRAP);
    label.setText("This change suggestion is not applicable as the current code on the server cannot be found in the "
      + "file.");
    // INFO: When resized to be too small, this icon won't be shown anymore!
    label.setImage(SonarLintImages.IMG_WARNING);
    label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
  }
}
