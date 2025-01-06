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
package org.sonarlint.eclipse.its.cdt;

import org.junit.Test;
import org.sonarlint.eclipse.its.shared.AbstractSonarLintTest;
import org.sonarlint.eclipse.its.shared.reddeer.perspectives.CppPerspective;
import org.sonarlint.eclipse.its.shared.reddeer.views.OnTheFlyView;

import static org.assertj.core.api.Assertions.tuple;

/**
 *  These tests are for the CDT integration of the plug-in. It should work correctly with different kinds of C/C++
 *  projects that are supported by CDT itself or other tools generating the Eclipse project files.
 */
public class CdtIntegrationTest extends AbstractSonarLintTest {
  @Test
  public void test_makefile_based_project() {
    // i) Open C/C++ perspective and import project
    new CppPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("cdt/MakefileProject", "MakefileProject");

    // ii) Open file and await for the analysis to show issues
    openFileAndWaitForAnalysisCompletion(rootProject.getResource("hello.c"));
    var onTheFlyView = new OnTheFlyView();
    onTheFlyView.open();
    waitForSonarLintMarkers(onTheFlyView,
      tuple("Complete the task associated to this \"TODO\" comment.", "hello.c", "few seconds ago"));
  }
}
