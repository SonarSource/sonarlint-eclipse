/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonarlint.eclipse.its.reddeer.conditions;

import java.util.List;
import org.eclipse.reddeer.common.condition.WaitCondition;
import org.sonarlint.eclipse.its.reddeer.views.OnTheFlyView;
import org.sonarlint.eclipse.its.reddeer.views.SonarLintIssue;

public class OnTheFlyViewIsEmpty implements WaitCondition {
  private final OnTheFlyView issuesView;

  public OnTheFlyViewIsEmpty(OnTheFlyView issuesView) {
    this.issuesView = issuesView;
  }

  @Override
  public boolean test() {
    return issuesView.getIssues().isEmpty();
  }

  @Override
  public List<SonarLintIssue> getResult() {
    return issuesView.getIssues();
  }

  @Override
  public String errorMessageWhile() {
    return "On-The-Fly issue list is still empty";
  }

  @Override
  public String errorMessageUntil() {
    return "On-The-Fly issue list is still not empty";
  }

  @Override
  public String description() {
    return "on-the-fly issue list is empty";
  }
}
