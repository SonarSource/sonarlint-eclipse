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
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.swt.SWT;

public class FixSuggestionAvailableDialog extends DefaultShell {
  public FixSuggestionAvailableDialog(Integer index, Integer all) {
    super(String.format("SonarQube Fix Suggestion (%d/%d)", index + 1, all));
  }

  public void applyTheChange() {
    new ApplyTheChangeButton(this).click();
  }

  public void declineTheChange() {
    new DeclineTheChangeButton(this).click();
  }

  public void cancel() {
    new CancelButton(this).click();
  }

  private static class ApplyTheChangeButton extends PredefinedButton {
    public ApplyTheChangeButton(ReferencedComposite referencedComposite) {
      super(referencedComposite, 0, "Apply the change", SWT.PUSH);
    }
  }

  private static class DeclineTheChangeButton extends PredefinedButton {
    public DeclineTheChangeButton(ReferencedComposite referencedComposite) {
      super(referencedComposite, 0, "Decline the change", SWT.PUSH);
    }
  }

  private static class CancelButton extends PredefinedButton {
    public CancelButton(ReferencedComposite referencedComposite) {
      super(referencedComposite, 0, "Cancel", SWT.PUSH);
    }
  }
}
