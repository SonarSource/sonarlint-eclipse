/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonarlint.eclipse.its.shared.reddeer.dialogs;

import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.label.DefaultLabel;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.DefaultText;

public class ProjectSelectionDialog extends DefaultShell {
  public ProjectSelectionDialog() {
    super("SonarQube for Eclipse - Project Selection");
  }

  public void filterProjectName(String projectName) {
    new DefaultText(this).setText(projectName);
  }

  public void ok() {
    new OkButton(this).click();
  }

  public String getMessage() {
    return new DefaultLabel().getText();
  }
}
