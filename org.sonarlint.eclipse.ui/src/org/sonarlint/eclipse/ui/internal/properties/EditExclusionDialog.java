/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2020 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.properties;

import java.util.function.Supplier;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.sonarlint.eclipse.core.internal.resources.ExclusionItem;

public abstract class EditExclusionDialog extends TitleAreaDialog implements Supplier<ExclusionItem> {
  protected Button okButton;
  protected boolean editing;

  
  public EditExclusionDialog(Shell parentShell) {
    super(parentShell);
  }
  
  @Override
  public boolean isHelpAvailable() {
    return false;
  }

  /**
   * Adds buttons to this dialog's button bar.
   *
   * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar
   */
  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    okButton.setEnabled(editing);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
  }

  @Override
  protected boolean isResizable() {
    return true;
  }
  
  /**
   * Creates and configures this dialog's main composite.
   *
   * @param parentComposite
   *            parent's composite
   * @return this dialog's main composite
   */
  protected Composite createComposite(Composite parentComposite, int numColumns) {
    // creates a composite with standard margins and spacing
    Composite contents = new Composite(parentComposite, SWT.NONE);

    contents.setLayout(new GridLayout(numColumns, false));
    contents.setLayoutData(new GridData(GridData.FILL_BOTH));

    if (editing) {
      setTitle("Edit a File Exclusion");
    } else {
      setTitle("Create a New File Exclusion");
    }
    setMessage(standardMessage());
    return contents;
  }
  
  /**
   * Configures this dialog's shell, setting the shell's text.
   *
   * @see org.eclipse.jface.window.Window#configureShell(Shell)
   */
  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    if (editing) {
      shell.setText("Edit Exclusion");
    } else {
      shell.setText("Create Exclusion");
    }
  }

  protected abstract String standardMessage();
}
