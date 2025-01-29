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

import org.eclipse.reddeer.common.condition.AbstractWaitCondition;
import org.sonarlint.eclipse.its.shared.reddeer.views.SonarLintConsole;

/**
 *  Rely on the SonarQube Console that there is no issue found on the project, will always appear
 *  on bound projects that have no issue directly after the connection is fully synchronized.
 */
public class ZeroIssuesOnProject extends AbstractWaitCondition {
  private final String projectName;

  public ZeroIssuesOnProject(String projectName) {
    this.projectName = projectName;
  }

  @Override
  public boolean test() {
    var index = new SonarLintConsole().getConsoleView().getConsoleText()
      .lastIndexOf("Found 0 issue(s) on project '" + projectName + "'");
    return index > -1;
  }
}
