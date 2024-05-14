/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.preference.StringButtonFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;

/** This is used as a base for all path based preferences */
public abstract class AbstractPathField extends StringButtonFieldEditor {
  private final boolean requireFolder;

  protected AbstractPathField(String preferenceName, String labelText, Composite parent, boolean requireFolder) {
    super(preferenceName, labelText, parent);
    setChangeButtonText("Browse ...");
    this.requireFolder = requireFolder;
  }

  @Override
  protected void doFillIntoGrid(Composite parent, int numColumns) {
    super.doFillIntoGrid(parent, numColumns);
    provideDefaultValue();
  }

  @Override
  protected boolean doCheckState() {
    var stringValue = getStringValue();
    if (stringValue.isBlank()) {
      return true;
    }

    Path path;
    try {
      path = Paths.get(stringValue);
    } catch (InvalidPathException e) {
      setErrorMessage("Invalid path: " + stringValue);
      return false;
    }
    if (!Files.exists(path)) {
      setErrorMessage("Doesn't exist: " + stringValue);
      return false;
    }
    return checkStateFurther(path);
  }

  @Nullable
  @Override
  protected String changePressed() {
    String fileOrFolder;
    if (requireFolder) {
      var dialog = new DirectoryDialog(getShell(), SWT.OPEN);
      fileOrFolder = dialog.open();
    } else {
      var dialog = new FileDialog(getShell(), SWT.OPEN);
      if (getStringValue().trim().length() > 0) {
        dialog.setFileName(getStringValue());
      }
      fileOrFolder = dialog.open();
    }

    if (fileOrFolder != null) {
      fileOrFolder = fileOrFolder.trim();
      if (fileOrFolder.length() > 0) {
        return fileOrFolder;
      }
    }
    return null;
  }

  /** The default value should be provided for the user to see instead of a blank field */
  abstract void provideDefaultValue();

  /**
   *  After checking if path is correct and file/folder exists, we have to do some further checks
   *  (e.g. is there actually a Java executable present) in special occasions
   *
   *  @param value chosen by the user for us to test
   *  @return true if the check succeeds, false otherwise
   */
  abstract boolean checkStateFurther(Path value);
}
