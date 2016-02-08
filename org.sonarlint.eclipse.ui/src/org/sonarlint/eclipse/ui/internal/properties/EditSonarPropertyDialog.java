/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProperty;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;

public class EditSonarPropertyDialog extends StatusDialog {

  private SonarLintProperty sonarProperty;

  private Text fNameText;
  private Text fValueText;

  private StatusInfo fValidationStatus;
  // https://bugs.eclipse.org/bugs/show_bug.cgi?id=4354
  private boolean fSuppressError = true;

  private boolean fIsNameModifiable;

  /**
   * Creates a new dialog.
   *
   * @param parent the shell parent of the dialog
   * @param property the property to edit
   * @param edit whether this is a new property or an existing being edited
   * @param isNameModifiable whether the name of the property may be modified
   */
  public EditSonarPropertyDialog(Shell parent, SonarLintProperty property, boolean edit, boolean isNameModifiable) {
    super(parent);

    String title = edit
      ? "Edit property"
      : "New property";
    setTitle(title);

    sonarProperty = property;
    fIsNameModifiable = isNameModifiable;

    fValidationStatus = new StatusInfo();

  }

  @Override
  protected boolean isResizable() {
    return true;
  }

  @Override
  public void create() {
    super.create();
    updateStatusAndButtons();
    getButton(IDialogConstants.OK_ID).setEnabled(getStatus().isOK());
  }

  @Override
  protected Control createDialogArea(Composite ancestor) {
    Composite parent = new Composite(ancestor, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    parent.setLayout(layout);
    parent.setLayoutData(new GridData(GridData.FILL_BOTH));

    createLabel(parent, "Name:");

    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    layout = new GridLayout();
    layout.numColumns = 1;
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    composite.setLayout(layout);

    fNameText = createNameText(composite);
    if (fIsNameModifiable) {
      fNameText.addFocusListener(new FocusListener() {
        @Override
        public void focusGained(FocusEvent e) {
          // Nothing to do
        }

        @Override
        public void focusLost(FocusEvent e) {
          if (fSuppressError) {
            fSuppressError = false;
            updateStatusAndButtons();
          }
        }
      });
    } else {
      fNameText.setEditable(false);
    }

    createLabel(parent, "Value:");

    fValueText = new Text(parent, SWT.BORDER);
    fValueText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    fValueText.setText(sonarProperty.getValue());
    fNameText.setText(sonarProperty.getName());
    if (fIsNameModifiable) {
      ModifyListener listener = new ModifyListener() {
        @Override
        public void modifyText(ModifyEvent e) {
          fSuppressError = false;
          updateStatusAndButtons();
        }
      };
      fNameText.addModifyListener(listener);
    }

    applyDialogFont(parent);
    return parent;
  }

  private static Label createLabel(Composite parent, String name) {
    Label label = new Label(parent, SWT.NULL);
    label.setText(name);
    label.setLayoutData(new GridData());

    return label;
  }

  private Text createNameText(Composite parent) {
    int descFlags = fIsNameModifiable ? SWT.BORDER : (SWT.BORDER | SWT.READ_ONLY);
    Text text = new Text(parent, descFlags);
    final GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.widthHint = convertWidthInCharsToPixels(20);
    text.setLayoutData(gd);
    return text;
  }

  @Override
  protected void okPressed() {
    String name = fNameText == null ? sonarProperty.getName() : fNameText.getText();
    sonarProperty = new SonarLintProperty(name, fValueText.getText());
    super.okPressed();
  }

  private void updateStatusAndButtons() {
    StatusInfo status = fValidationStatus;
    boolean isEmpty = fNameText != null && fNameText.getText().length() == 0;
    if (!fSuppressError && isEmpty) {
      status = new StatusInfo();
      status.setError("Please input a name");
    } else if (fNameText != null && !isValidPropertyName(fNameText.getText())) {
      status = new StatusInfo();
      status.setError("Invalid property name");
    }
    updateStatus(status);
  }

  /**
   * Checks whether the given string is a valid
   * property name.
   *
   * @param name the string to test
   * @return <code>true</code> if the name is valid
   */
  private static boolean isValidPropertyName(String name) {
    return !name.contains(" ");
  }

  /**
   * Returns the created property.
   *
   * @return the created property
   */
  public SonarLintProperty getSonarProperty() {
    return sonarProperty;
  }

  @Override
  protected Point getInitialSize() {
    Point defaultSize = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
    Point restoredSize = super.getInitialSize();
    if (defaultSize.x > restoredSize.x) {
      restoredSize.x = defaultSize.x;
    }
    return restoredSize;
  }

  @Override
  protected IDialogSettings getDialogBoundsSettings() {
    String sectionName = getClass().getName() + "_dialogBounds"; //$NON-NLS-1$
    IDialogSettings settings = SonarLintUiPlugin.getDefault().getDialogSettings();
    IDialogSettings section = settings.getSection(sectionName);
    if (section == null) {
      section = settings.addNewSection(sectionName);
    }
    return section;
  }
}
