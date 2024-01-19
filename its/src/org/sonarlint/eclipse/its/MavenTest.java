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
package org.sonarlint.eclipse.its;

import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.eclipse.condition.ProjectExists;
import org.eclipse.reddeer.eclipse.jdt.ui.packageview.PackageExplorerPart;
import org.eclipse.reddeer.eclipse.ui.perspectives.JavaPerspective;
import org.eclipse.reddeer.workbench.impl.editor.DefaultEditor;
import org.eclipse.reddeer.workbench.impl.editor.Marker;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class MavenTest extends AbstractSonarLintTest {

  @Test
  public void shouldNotAnalyzeResourcesInNestedModules() {
    new JavaPerspective().open();
    importExistingProjectIntoWorkspace("java/maven");
    importExistingProjectIntoWorkspace("java/maven/sample-module1");
    importExistingProjectIntoWorkspace("java/maven/sample-module2");

    // Use package explorer to wait for module 1 since reddeer doesn't support hierarchical layout of project explorer
    // https://github.com/eclipse/reddeer/issues/2161
    var packageExplorer = new PackageExplorerPart();
    new WaitUntil(new ProjectExists("sample-module1", packageExplorer));
    var sampleModule1Project = packageExplorer.getProject("sample-module1");

    int previousAnalysisJobCount = scheduledAnalysisJobCount.get();
    var rootProject = packageExplorer.getProject("sample-maven");
    rootProject.getResource("sample-module1", "src", "main", "java", "hello", "Hello1.java").open();
    assertThat(scheduledAnalysisJobCount.get()).isEqualTo(previousAnalysisJobCount);
    var defaultEditor = new DefaultEditor();
    assertThat(defaultEditor.getMarkers()).isEmpty();
    defaultEditor.close();

    openFileAndWaitForAnalysisCompletion(sampleModule1Project.getResource("src/main/java", "hello", "Hello1.java"));
    defaultEditor = new DefaultEditor();
    assertThat(defaultEditor.getMarkers())
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsExactly(tuple("Replace this use of System.out by a logger.", 9));
    defaultEditor.close();

    if (!platformVersion().toString().startsWith("4.4") && !platformVersion().toString().startsWith("4.5")) {
      // Issues on pom.xml
      openFileAndWaitForAnalysisCompletion(rootProject.getResource("pom.xml"));
      defaultEditor = new DefaultEditor();
      assertThat(defaultEditor.getMarkers())
        .extracting(Marker::getText, Marker::getLineNumber)
        .containsExactly(tuple("Replace \"pom.name\" with \"project.name\".", 11));
      defaultEditor.close();
    }
  }

}
