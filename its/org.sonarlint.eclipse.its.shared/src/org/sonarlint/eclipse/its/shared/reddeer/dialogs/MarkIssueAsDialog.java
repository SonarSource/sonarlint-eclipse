/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2025 SonarSource SA
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

import org.eclipse.reddeer.core.reference.ReferencedComposite;
import org.eclipse.reddeer.swt.impl.button.PredefinedButton;
import org.eclipse.reddeer.swt.impl.button.RadioButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.DefaultText;
import org.eclipse.swt.SWT;

public class MarkIssueAsDialog extends DefaultShell {
  public MarkIssueAsDialog() {
    super("Mark Issue as Resolved on SonarQube Server");
  }

  public void selectWontFix() {
    new RadioButton(this).click();
  }

  public void selectFalsePositive() {
    new RadioButton(this, 1).click();
  }

  public void setComment(String comment) {
    new DefaultText(this).setText(comment);
  }

  public void ok() {
    new MarkAsResolvedButton(this).click();
  }

  private static class MarkAsResolvedButton extends PredefinedButton {
    public MarkAsResolvedButton(ReferencedComposite referencedComposite) {
      super(referencedComposite, 0, "Mark Issue as Resolved", SWT.PUSH);
    }
  }
}
