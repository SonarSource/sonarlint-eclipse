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
package org.sonarlint.eclipse.its.standalone;

import org.eclipse.reddeer.eclipse.core.resources.Project;
import org.eclipse.reddeer.eclipse.ui.perspectives.JavaPerspective;
import org.eclipse.reddeer.workbench.impl.editor.TextEditor;
import org.junit.Before;
import org.junit.Test;
import org.sonarlint.eclipse.its.shared.AbstractSonarLintTest;
import org.sonarlint.eclipse.its.shared.reddeer.views.IssueLocationsView;
import org.sonarlint.eclipse.its.shared.reddeer.views.OnTheFlyView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class SecondaryLocationsTest extends AbstractSonarLintTest {

  private static OnTheFlyView onTheFlyView;
  private static IssueLocationsView locationsView;
  private static Project rootProject;

  @Before
  public void importProject() {
    new JavaPerspective().open();
    locationsView = new IssueLocationsView();
    locationsView.open();
    onTheFlyView = new OnTheFlyView();
    onTheFlyView.open();
    rootProject = importExistingProjectIntoWorkspace("java/java-multiple-flows", "java-multiple-flows");
  }

  @Test
  public void shouldShowIssueWithSingleLocation() {
    openAndAnalyzeFile("SimpleIssue.java");

    waitForSonarLintMarkers(onTheFlyView,
      tuple("Complete the task associated to this TODO comment.", "SimpleIssue.java", "few seconds ago"));
    onTheFlyView.selectItem(0);

    var flowItems = locationsView.getTree().getItems();
    assertThat(flowItems).hasSize(1);

    var locationRoot = flowItems.get(0);
    assertThat(locationRoot.getText()).isEqualTo("No additional locations associated with this issue");
    locationRoot.select();
    // SLE-479 No exception should be thrown
  }

  @Test
  public void shouldShowSingleFlow() {
    var helloEditor = openAndAnalyzeFile("SingleFlow.java");

    var issueTitle = "\"NullPointerException\" will be thrown when invoking method \"doAnotherThingWith()\".";
    waitForSonarLintMarkers(onTheFlyView,
      tuple(issueTitle + " [+5 locations]", "SingleFlow.java", "few seconds ago"));
    onTheFlyView.selectItem(0);

    var flowItems = locationsView.getTree().getItems();
    assertThat(flowItems).hasSize(1);

    var locationRoot = flowItems.get(0);
    assertThat(locationRoot.getText()).isEqualTo(issueTitle);
    assertThat(locationRoot.getItems()).hasSize(5);

    locationRoot.getItems().get(0).doubleClick();
    assertThat(helloEditor.getSelectedText()).isEqualTo("arg = null");
    assertThat(helloEditor.getCursorPosition().x).isEqualTo(21);
  }

  @Test
  public void shouldShowHighlightsOnly() {
    openAndAnalyzeFile("HighlightOnly.java");

    var issueTitle = "Remove these useless parentheses.";
    waitForSonarLintMarkers(onTheFlyView,
      tuple(issueTitle + " [+1 location]", "HighlightOnly.java", "few seconds ago"));
    onTheFlyView.selectItem(0);

    var allItems = locationsView.getTree().getItems();
    assertThat(allItems).hasSize(1);

    var locationRoot = allItems.get(0);
    assertThat(locationRoot.getText()).isEqualTo(issueTitle);
    assertThat(locationRoot.getItems()).isEmpty();
  }

  @Test
  public void shouldShowMultipleFlows() {
    var helloEditor = openAndAnalyzeFile("MultiFlows.java");

    var issueTitle = "\"NullPointerException\" will be thrown when invoking method \"doAnotherThingWith()\".";
    waitForSonarLintMarkers(onTheFlyView,
      tuple(issueTitle + " [+2 flows]", "MultiFlows.java", "few seconds ago"));
    onTheFlyView.selectItem(0);

    var allItems = locationsView.getTree().getItems();
    assertThat(allItems).hasSize(1);

    var locationRoot = allItems.get(0);
    assertThat(locationRoot.getText()).isEqualTo(issueTitle);
    assertThat(locationRoot.getItems()).hasSize(2);

    var flow1 = locationRoot.getItem("Flow 1");
    assertThat(flow1.getItems()).hasSize(5);

    var flow2 = locationRoot.getItem("Flow 2");
    assertThat(flow2.getItems()).hasSize(5);

    // Flows are not ordered, we can only check that the first nodes do not point to the same location

    flow1.getItems().get(0).doubleClick();
    assertThat(helloEditor.getSelectedText()).isEqualTo("arg = null");
    var flow1Line = helloEditor.getCursorPosition().x;

    flow2.getItems().get(0).doubleClick();
    assertThat(helloEditor.getSelectedText()).isEqualTo("arg = null");
    var flow2Line = helloEditor.getCursorPosition().x;

    assertThat(flow1Line).isNotEqualTo(flow2Line);
  }

  @Test
  public void shouldShowFlattenedFlows() {
    var cognitiveComplexityEditor = openAndAnalyzeFile("CognitiveComplexity.java");

    var issueTitle = "Refactor this method to reduce its Cognitive Complexity from 24 to the 15 allowed.";
    waitForSonarLintMarkers(onTheFlyView,
      tuple(issueTitle + " [+15 locations]", "CognitiveComplexity.java", "few seconds ago"));
    onTheFlyView.selectItem(0);

    var allItems = locationsView.getTree().getItems();
    assertThat(allItems).hasSize(1);

    var locationRoot = allItems.get(0);
    assertThat(locationRoot.getText()).isEqualTo(issueTitle);
    var allNodes = locationRoot.getItems();
    assertThat(allNodes).hasSize(15);

    allNodes.get(0).doubleClick();
    assertThat(cognitiveComplexityEditor.getSelectedText()).isEqualTo("if");
    assertThat(cognitiveComplexityEditor.getCursorPosition()).extracting(p -> p.x, p -> p.y).containsExactly(18, 4);

    allNodes.get(14).doubleClick();
    assertThat(cognitiveComplexityEditor.getSelectedText()).isEqualTo("else");
    assertThat(cognitiveComplexityEditor.getCursorPosition()).extracting(p -> p.x, p -> p.y).containsExactly(45, 8);
  }

  private TextEditor openAndAnalyzeFile(String fileName) {
    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "hello", fileName));
    return new TextEditor();
  }
}
