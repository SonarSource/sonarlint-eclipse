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
package org.sonarlint.eclipse.its.shared.reddeer.conditions;

import org.eclipse.reddeer.common.condition.WaitCondition;
import org.sonarlint.eclipse.its.shared.reddeer.views.RuleDescriptionView;

public class RuleDescriptionViewOpenedWithContent implements WaitCondition {
  private final RuleDescriptionView ruleDescriptionView;
  private final String content;

  public RuleDescriptionViewOpenedWithContent(RuleDescriptionView ruleDescriptionView, String content) {
    this.ruleDescriptionView = ruleDescriptionView;
    this.content = content;
  }

  @Override
  public boolean test() {
    if (!ruleDescriptionView.isOpen()) {
      return false;
    }
    ruleDescriptionView.open();

    return ruleDescriptionView.getFlatTextContent().contains(content);
  }

  @Override
  public RuleDescriptionView getResult() {
    return ruleDescriptionView;
  }

  @Override
  public String description() {
    return "Rule Description view is opened with content '" + content + "'";
  }

  @Override
  public String errorMessageWhile() {
    return "Rule Description view is still opened with content '" + content + "'";
  }

  @Override
  public String errorMessageUntil() {
    return "Rule Description view is not yet opened with content '" + content + "'";
  }
}
