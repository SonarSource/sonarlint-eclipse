/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2023 SonarSource SA
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

import org.eclipse.reddeer.eclipse.jdt.ui.javaeditor.JavaEditor;
import org.eclipse.reddeer.eclipse.ui.perspectives.JavaPerspective;
import org.eclipse.reddeer.workbench.impl.editor.TextEditor;
import org.junit.Test;
import org.sonarlint.eclipse.its.reddeer.views.OnTheFlyView;
import org.sonarlint.eclipse.its.reddeer.views.SonarLintIssueMarker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class LocalLeakTest extends AbstractSonarLintTest {

  @Test
  public void shouldComputeLocalLeak() {
    new JavaPerspective().open();

    var issuesView = new OnTheFlyView();
    issuesView.open();

    assertThat(issuesView.getProblemColumns()).containsExactly("Date", "Description", "Resource");
    assertThat(issuesView.getIssues()).isEmpty();

    var javaSimple = importExistingProjectIntoWorkspace("java/leak", "leak");

    var helloFile = javaSimple.getProjectItem("src", "hello", "Hello.java");
    openFileAndWaitForAnalysisCompletion(helloFile);

    var sonarlintIssues = issuesView.getIssues();

    assertThat(sonarlintIssues).extracting(SonarLintIssueMarker::getResource, SonarLintIssueMarker::getDescription, SonarLintIssueMarker::getCreationDate)
      .containsOnly(tuple("Hello.java", "Replace this use of System.out by a logger.", ""));

    // Change content
    var javaEditor = new JavaEditor("Hello.java");
    javaEditor.insertText(7, 43, "\nSystem.out.println(\"Hello1\");");
    doAndWaitForSonarLintAnalysisJob(() -> javaEditor.save());

    sonarlintIssues = issuesView.getIssues();

    assertThat(sonarlintIssues).extracting(SonarLintIssueMarker::getResource, SonarLintIssueMarker::getDescription, SonarLintIssueMarker::getCreationDate)
      .containsOnly(tuple("Hello.java", "Replace this use of System.out by a logger.", ""),
        tuple("Hello.java", "Replace this use of System.out by a logger.", "few seconds ago"));
  }

  @Test
  public void dontLooseLeakOnParsingError() {
    // Don't run this test on macOS devices as Node.js might not be found!
    ignoreMacOS();
    
    new JavaPerspective().open();

    var issuesView = new OnTheFlyView();
    issuesView.open();

    var jsSimple = importExistingProjectIntoWorkspace("js/js-simple", "js-simple");

    var helloFile = jsSimple.getProjectItem("src", "hello.js");
    openFileAndWaitForAnalysisCompletion(helloFile);

    var sonarlintIssues = issuesView.getIssues();

    assertThat(sonarlintIssues).extracting(SonarLintIssueMarker::getResource, SonarLintIssueMarker::getDescription, SonarLintIssueMarker::getCreationDate)
      .containsOnly(tuple("hello.js", "Multiline support is limited to browsers supporting ES5 only.", ""));

    // Change content
    var textEditor = new TextEditor("hello.js");
    textEditor.insertText(2, 17, "\nlet i;");
    doAndWaitForSonarLintAnalysisJob(() -> textEditor.save());

    sonarlintIssues = issuesView.getIssues();

    assertThat(sonarlintIssues).extracting(SonarLintIssueMarker::getResource, SonarLintIssueMarker::getDescription, SonarLintIssueMarker::getCreationDate)
      .containsOnly(tuple("hello.js", "Multiline support is limited to browsers supporting ES5 only.", ""),
        tuple("hello.js", "Remove the declaration of the unused 'i' variable.", "few seconds ago"));

    // Insert content that should crash analyzer
    var beforeCrash = textEditor.getText();
    textEditor.insertText(3, 8, "\nbl ah");
    doAndWaitForSonarLintAnalysisJob(() -> textEditor.save());

    // Issues are still there
    sonarlintIssues = issuesView.getIssues();

    assertThat(sonarlintIssues).extracting(SonarLintIssueMarker::getResource, SonarLintIssueMarker::getDescription, SonarLintIssueMarker::getCreationDate)
      .containsOnly(tuple("hello.js", "Multiline support is limited to browsers supporting ES5 only.", ""),
        tuple("hello.js", "Remove the declaration of the unused 'i' variable.", "few seconds ago"));

    // Fix parsing issue
    textEditor.setText(beforeCrash);
    doAndWaitForSonarLintAnalysisJob(() -> textEditor.save());

    sonarlintIssues = issuesView.getIssues();

    assertThat(sonarlintIssues).extracting(SonarLintIssueMarker::getResource, SonarLintIssueMarker::getDescription, SonarLintIssueMarker::getCreationDate)
      .containsOnly(tuple("hello.js", "Multiline support is limited to browsers supporting ES5 only.", ""),
        tuple("hello.js", "Remove the declaration of the unused 'i' variable.", "few seconds ago"));

  }

}
