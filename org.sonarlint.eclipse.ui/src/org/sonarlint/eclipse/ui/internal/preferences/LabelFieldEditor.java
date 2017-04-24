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
package org.sonarlint.eclipse.ui.internal.preferences;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * Fake field editor allowing to display long text
 */
public class LabelFieldEditor extends FieldEditor {

  private Label label;

  public LabelFieldEditor(String name, String labelText, Composite parent) {
    init(name, labelText);
    createControl(parent);
  }

  @Override
  protected void adjustForNumColumns(int numColumns) {
    ((GridData) label.getLayoutData()).horizontalSpan = numColumns;
  }

  @Override
  protected void doFillIntoGrid(Composite parent, int numColumns) {
    Label theLabel = getLabelControl(parent);
    ((GridData) theLabel.getLayoutData()).horizontalSpan = numColumns;
  }

  @Override
  public Label getLabelControl(Composite parent) {
    if (label == null) {
      label = new Label(parent, SWT.WRAP);
      GridData gd = new GridData(SWT.LEFT, SWT.CENTER, true, false);
      label.setLayoutData(gd);
      label.setFont(parent.getFont());
      String text = getLabelText();
      if (text != null) {
        label.setText(text);
      }
      label.addDisposeListener(event -> label = null);
    } else {
      checkParent(label, parent);
    }
    return label;
  }

  @Override
  protected void doLoad() {
    // Nothing to do
  }

  @Override
  protected void doLoadDefault() {
    // Nothing to do
  }

  @Override
  protected void doStore() {
    // Nothing to do
  }

  @Override
  public int getNumberOfControls() {
    return 1;
  }
}
