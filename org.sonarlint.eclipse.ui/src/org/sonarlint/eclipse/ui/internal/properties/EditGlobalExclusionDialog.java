/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2017 SonarSource SA
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

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;

import javax.annotation.Nullable;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.sonarlint.eclipse.core.internal.resources.ExclusionItem;
import org.sonarlint.eclipse.core.internal.resources.ExclusionItem.Type;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;

public class EditGlobalExclusionDialog extends EditExclusionDialog {
  private Button okButton;

  private Text field;

  private String editItem;
  private boolean editing;
  private boolean initialized = false;

  /**
   * The standard message to be shown when there are no problems being reported.
   */
  private static final String STANDARD_MESSAGE = "Define the GLOB pattern to exclude files from SonarLint analyses";

  public EditGlobalExclusionDialog(Shell parentShell, @Nullable String editItem) {
    super(parentShell);
    this.editing = (editItem != null);
    this.editItem = editItem;
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

  /**
   * Creates and returns the contents of this dialog (except for the button bar).
   *
   * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    // top level composite
    Composite parentComposite = (Composite) super.createDialogArea(parent);

    initializeDialogUnits(parentComposite);

    // creates dialog area composite
    Composite contents = createComposite(parentComposite);

    // creates and lay outs dialog area widgets
    createWidgets(contents);

    Dialog.applyDialogFont(parentComposite);
    initialized = true;
    return contents;
  }

  /**
   * Creates and configures this dialog's main composite.
   *
   * @param parentComposite
   *            parent's composite
   * @return this dialog's main composite
   */
  private Composite createComposite(Composite parentComposite) {
    // creates a composite with standard margins and spacing
    Composite contents = new Composite(parentComposite, SWT.NONE);

    contents.setLayout(new GridLayout(1, false));
    contents.setLayoutData(new GridData(GridData.FILL_BOTH));

    if (editing) {
      setTitle("Edit a File Exclusion");
    } else {
      setTitle("Create a New File Exclusion");
    }
    setMessage(STANDARD_MESSAGE);
    return contents;
  }

  /**
   * Creates widgets for this dialog.
   *
   * @param contents
   *            the parent composite where to create widgets
   */
  private void createWidgets(Composite contents) {
    field = new Text(contents, SWT.SINGLE | SWT.BORDER);
    field.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    ModifyListener fieldListener = e -> onFieldChanged();
    field.addModifyListener(fieldListener);
  }

  private void onFieldChanged() {
    editItem = field.getText();
    validate();
  }

  private void validate() {
    if (!initialized) {
      return;
    }
    String validationMessage = STANDARD_MESSAGE;
    int validationStatus = IMessageProvider.NONE;

    if (StringUtils.isEmpty(editItem)) {
      validationMessage = "The field is empty";
      validationStatus = IMessageProvider.ERROR;
    }

    try {
      FileSystem fs = FileSystems.getDefault();
      fs.getPathMatcher("glob:" + editItem);
    } catch (Exception e) {
      validationMessage = "The pattern has an invalid syntax";
      validationStatus = IMessageProvider.ERROR;
    }

    setMessage(validationMessage, validationStatus);
    if (validationStatus != IMessageProvider.ERROR) {
      okButton.setEnabled(true);
    } else {
      okButton.setEnabled(false);
    }
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

  @Override
  public ExclusionItem get() {
    return new ExclusionItem(Type.GLOB, editItem);
  }
}
