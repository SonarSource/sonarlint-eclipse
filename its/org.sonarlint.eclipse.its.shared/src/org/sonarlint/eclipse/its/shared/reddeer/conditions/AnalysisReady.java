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

/** Await analysis to be ready without the project ever getting unready */
public class AnalysisReady extends AbstractReadyCondition {
  public AnalysisReady(String projectName) {
    super(projectName);
  }

  @Override
  public boolean test() {
    var consoleText = consoleView.getConsoleText();

    var indexFalse = consoleText.lastIndexOf(falsePattern);
    var indexTrue = consoleText.lastIndexOf(truePattern);
    // returns true if ready=true is more recent
    return indexTrue > indexFalse;
  }
}
