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
package org.sonarlint.eclipse.its.reddeer.wizards;

import org.eclipse.reddeer.swt.impl.button.NoButton;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.button.YesButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;

/** We use {@link org.eclipse.jface.dialogs.MessageDialog} to display messages on that feature */
public class OpenInIdeDialog extends DefaultShell {
  public OpenInIdeDialog() {
    super("Open in IDE");
  }
  
  public void ok() {
    new OkButton(this).click();
  }
  
  public void no() {
    new NoButton(this).click();
  }
  
  public void yes() {
    new YesButton(this).click();
  }
}
