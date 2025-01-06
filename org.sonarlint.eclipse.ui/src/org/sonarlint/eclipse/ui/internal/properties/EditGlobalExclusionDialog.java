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
package org.sonarlint.eclipse.ui.internal.properties;

import java.nio.file.FileSystems;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.sonarlint.eclipse.core.internal.resources.ExclusionItem;
import org.sonarlint.eclipse.core.internal.resources.ExclusionItem.Type;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;

public class EditGlobalExclusionDialog extends EditExclusionDialog {
  private Text field;

  private String editItem;
  private boolean initialized = false;

  /**
   * The standard message to be shown when there are no problems being reported.
   */
  private static final String STANDARD_MESSAGE = "Define the GLOB pattern to exclude files from the analyses";

  public EditGlobalExclusionDialog(Shell parentShell, @Nullable String editItem) {
    super(parentShell);
    this.editing = (editItem != null);
    this.editItem = editItem;
  }

  @Override
  public String standardMessage() {
    return STANDARD_MESSAGE;
  }

  /**
   * Creates and returns the contents of this dialog (except for the button bar).
   *
   * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    // top level composite
    var parentComposite = (Composite) super.createDialogArea(parent);

    initializeDialogUnits(parentComposite);

    // creates dialog area composite
    var contents = createComposite(parentComposite, 1);

    // creates and lay outs dialog area widgets
    createWidgets(contents);

    Dialog.applyDialogFont(parentComposite);
    initialized = true;
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
    field.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    if (editing) {
      field.setText(editItem);
    }

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
    var validationMessage = STANDARD_MESSAGE;
    var validationStatus = IMessageProvider.NONE;

    if (StringUtils.isEmpty(editItem)) {
      validationMessage = "The field is empty";
      validationStatus = IMessageProvider.ERROR;
    }

    try {
      var fs = FileSystems.getDefault();
      fs.getPathMatcher("glob:" + editItem);
    } catch (Exception e) {
      validationMessage = "The pattern has an invalid syntax";
      validationStatus = IMessageProvider.ERROR;
    }

    setMessage(validationMessage, validationStatus);
    okButton.setEnabled(validationStatus != IMessageProvider.ERROR);
  }

  @Override
  public ExclusionItem get() {
    return new ExclusionItem(Type.GLOB, editItem);
  }
}
