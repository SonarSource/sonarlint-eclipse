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
package org.sonarlint.eclipse.core.internal.quickfixes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MarkerQuickFix {

  private final String message;
  private final List<MarkerTextEdit> textEdits = new ArrayList<>();

  public MarkerQuickFix(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  public void addTextEdit(MarkerTextEdit textEdit) {
    textEdits.add(textEdit);
  }

  public List<MarkerTextEdit> getTextEdits() {
    return Collections.unmodifiableList(textEdits);
  }

  public boolean isValid() {
    return textEdits.stream().allMatch(MarkerTextEdit::isValid);
  }

}
