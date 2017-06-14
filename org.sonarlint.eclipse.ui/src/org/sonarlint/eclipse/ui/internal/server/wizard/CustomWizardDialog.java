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
package org.sonarlint.eclipse.ui.internal.server.wizard;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

public class CustomWizardDialog extends WizardDialog {
  private ServerConnectionWizard wizardWithNextHook;
  private boolean movingBackward;

  public CustomWizardDialog(Shell parentShell, ServerConnectionWizard wizardWithNextHook) {
    super(parentShell, wizardWithNextHook);
    this.wizardWithNextHook = wizardWithNextHook;
  }

  @Override
  protected void nextPressed() {
    if (!wizardWithNextHook.beforeNextPressed()) {
      return;
    }
    super.nextPressed();
  }

  @Override
  protected void backPressed() {
    this.movingBackward = true;
    super.backPressed();
    this.movingBackward = false;
  }

  public boolean isMovingBackward() {
    return movingBackward;
  }
}
