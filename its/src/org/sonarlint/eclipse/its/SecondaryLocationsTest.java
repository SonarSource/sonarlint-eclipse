/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2021 SonarSource SA
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

import java.util.List;
import org.eclipse.reddeer.eclipse.core.resources.DefaultProject;
import org.eclipse.reddeer.eclipse.jdt.ui.packageview.PackageExplorerPart;
import org.eclipse.reddeer.eclipse.ui.perspectives.JavaPerspective;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.workbench.impl.editor.TextEditor;
import org.junit.Before;
import org.junit.Test;
import org.sonarlint.eclipse.its.reddeer.views.IssueLocationsView;
import org.sonarlint.eclipse.its.reddeer.views.OnTheFlyView;
import org.sonarlint.eclipse.its.reddeer.views.SonarLintIssue;

import static org.assertj.core.api.Assertions.assertThat;

public class SecondaryLocationsTest extends AbstractSonarLintTest {

  private static OnTheFlyView onTheFlyView;
  private static IssueLocationsView locationsView;

  @Before
  public void importProject() {
    new JavaPerspective().open();
    locationsView = new IssueLocationsView();
    locationsView.open();
    onTheFlyView = new OnTheFlyView();
    onTheFlyView.open();
    importExistingProjectIntoWorkspace("java/java-multiple-flows");
  }

  @Test
  public void shouldShowSingleFlow() {
    TextEditor helloEditor = openAndAnalyzeFile("SingleFlow.java");

    String issueTitle = "\"NullPointerException\" will be thrown when invoking method \"doAnotherThingWith()\".";
    assertThat(onTheFlyView.getIssues())
      .extracting(SonarLintIssue::getDescription)
      .containsOnly(issueTitle + " [+5 locations]");
    onTheFlyView.getItems().get(0).select();

    List<TreeItem> flowItems = locationsView.getTree().getItems();
    assertThat(flowItems).hasSize(1);

    TreeItem locationRoot = flowItems.get(0);
    assertThat(locationRoot.getText()).isEqualTo(issueTitle);
    assertThat(locationRoot.getItems()).hasSize(5);

    locationRoot.getItems().get(0).doubleClick();
    assertThat(helloEditor.getSelectedText()).isEqualTo("arg = null");
    assertThat(helloEditor.getCursorPosition().x).isEqualTo(21);
  }

  @Test
  public void shouldShowHighlightsOnly() {
    openAndAnalyzeFile("HighlightOnly.java");

    String issueTitle = "Remove these useless parentheses.";
    assertThat(onTheFlyView.getIssues())
      .extracting(SonarLintIssue::getDescription)
      .containsOnly(issueTitle + " [+1 location]");
    onTheFlyView.getItems().get(0).select();

    List<TreeItem> allItems = locationsView.getTree().getItems();
    assertThat(allItems).hasSize(1);

    TreeItem locationRoot = allItems.get(0);
    assertThat(locationRoot.getText()).isEqualTo(issueTitle);
    assertThat(locationRoot.getItems()).isEmpty();
  }

  @Test
  public void shouldShowMultipleFlows() {
    TextEditor helloEditor = openAndAnalyzeFile("MultiFlows.java");

    String issueTitle = "\"NullPointerException\" will be thrown when invoking method \"doAnotherThingWith()\".";
    assertThat(onTheFlyView.getIssues())
      .extracting(SonarLintIssue::getDescription)
      .containsOnly(issueTitle + " [+2 flows]");
    onTheFlyView.getItems().get(0).select();

    List<TreeItem> allItems = locationsView.getTree().getItems();
    assertThat(allItems).hasSize(1);

    TreeItem locationRoot = allItems.get(0);
    assertThat(locationRoot.getText()).isEqualTo(issueTitle);
    assertThat(locationRoot.getItems()).hasSize(2);

    TreeItem flow1 = locationRoot.getItem("Flow 1");
    assertThat(flow1.getItems()).hasSize(5);

    TreeItem flow2 = locationRoot.getItem("Flow 2");
    assertThat(flow2.getItems()).hasSize(5);

    // Flows are not ordered, we can only check that the first nodes do not point to the same location

    flow1.getItems().get(0).doubleClick();
    assertThat(helloEditor.getSelectedText()).isEqualTo("arg = null");
    int flow1Line = helloEditor.getCursorPosition().x;

    flow2.getItems().get(0).doubleClick();
    assertThat(helloEditor.getSelectedText()).isEqualTo("arg = null");
    int flow2Line = helloEditor.getCursorPosition().x;

    assertThat(flow1Line).isNotEqualTo(flow2Line);
  }

  @Test
  public void shouldShowFlattenedFlows() {
    TextEditor cognitiveComplexityEditor = openAndAnalyzeFile("CognitiveComplexity.java");

    String issueTitle = "Refactor this method to reduce its Cognitive Complexity from 24 to the 15 allowed.";
    assertThat(onTheFlyView.getIssues())
      .extracting(SonarLintIssue::getDescription)
      .containsOnly(issueTitle + " [+15 locations]");
    onTheFlyView.getItems().get(0).select();

    List<TreeItem> allItems = locationsView.getTree().getItems();
    assertThat(allItems).hasSize(1);

    TreeItem locationRoot = allItems.get(0);
    assertThat(locationRoot.getText()).isEqualTo(issueTitle);
    List<TreeItem> allNodes = locationRoot.getItems();
    assertThat(allNodes).hasSize(15);

    allNodes.get(0).doubleClick();
    assertThat(cognitiveComplexityEditor.getSelectedText()).isEqualTo("if");
    assertThat(cognitiveComplexityEditor.getCursorPosition()).extracting(p -> p.x, p -> p.y).containsExactly(18, 4);

    allNodes.get(14).doubleClick();
    assertThat(cognitiveComplexityEditor.getSelectedText()).isEqualTo("else");
    assertThat(cognitiveComplexityEditor.getCursorPosition()).extracting(p -> p.x, p -> p.y).containsExactly(45, 8);
  }

  public TextEditor openAndAnalyzeFile(String fileName) {
    PackageExplorerPart packageExplorer = new PackageExplorerPart();
    DefaultProject rootProject = packageExplorer.getProject("java-multiple-flows");
    doAndWaitForSonarLintAnalysisJob(() -> rootProject.getResource("src", "hello", fileName).open());
    return new TextEditor();
  }
}
