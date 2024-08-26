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
package org.sonarlint.eclipse.its.shared.reddeer.conditions;

import org.eclipse.reddeer.common.condition.WaitCondition;
import org.sonarlint.eclipse.its.shared.reddeer.dialogs.FixSuggestionUnavailableDialog;

public class FixSuggestionUnavailableDialogOpened implements WaitCondition {
  private final int index;
  private final int all;

  public FixSuggestionUnavailableDialogOpened(int index, int all) {
    this.index = index;
    this.all = all;
  }

  @Override
  public boolean test() {
    try {
      new FixSuggestionUnavailableDialog(index, all).isEnabled();
      return true;
    } catch (Exception ignored) {
      return false;
    }
  }

  @Override
  public FixSuggestionUnavailableDialog getResult() {
    return new FixSuggestionUnavailableDialog(index, all);
  }

  @Override
  public String description() {
    return "'Fix Suggestion (unavailable)' dialog is opened";
  }

  @Override
  public String errorMessageWhile() {
    return "'Fix Suggestion (unavailable)' dialog is still opened";
  }

  @Override
  public String errorMessageUntil() {
    return "'Fix Suggestion (unavailable)' dialog is not yet opened";
  }
}
