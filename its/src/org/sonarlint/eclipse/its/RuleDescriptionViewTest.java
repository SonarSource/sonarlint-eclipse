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

import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.eclipse.ui.markers.matcher.MarkerDescriptionMatcher;
import org.eclipse.reddeer.eclipse.ui.perspectives.JavaPerspective;
import org.eclipse.reddeer.workbench.impl.editor.DefaultEditor;
import org.eclipse.reddeer.workbench.impl.editor.Marker;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.sonarlint.eclipse.its.reddeer.conditions.RuleDescriptionViewIsLoaded;
import org.sonarlint.eclipse.its.reddeer.preferences.RuleConfigurationPreferences;
import org.sonarlint.eclipse.its.reddeer.views.OnTheFlyView;
import org.sonarlint.eclipse.its.reddeer.views.RuleDescriptionView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

public class RuleDescriptionViewTest extends AbstractSonarLintTest {

  @Test
  public void openRuleDescription() {
    new JavaPerspective().open();
    var ruleDescriptionView = new RuleDescriptionView();
    ruleDescriptionView.open();
    var onTheFlyView = new OnTheFlyView();
    onTheFlyView.open();

    var project = importExistingProjectIntoWorkspace("java/java-simple", "java-simple");

    openFileAndWaitForAnalysisCompletion(project.getResource("src", "hello", "Hello.java"));

    var defaultEditor = new DefaultEditor();
    assertThat(defaultEditor.getMarkers())
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsExactly(tuple("Replace this use of System.out or System.err by a logger.", 9));

    onTheFlyView.selectItem(0);
    ruleDescriptionView.open();

    new WaitUntil(new RuleDescriptionViewIsLoaded(ruleDescriptionView));
    var flatTextContent = ruleDescriptionView.getFlatTextContent();
    await().untilAsserted(() -> assertThat(flatTextContent).contains("java:S106"));
  }

  @Test
  public void openRuleDescription_with_educational_content() {
    var ruleConfigurationPreferences = RuleConfigurationPreferences.open();
    var monsterClassRule = ruleConfigurationPreferences.selectRule("java:S6539", "Java", "Classes should not depend on an excessive number of classes (aka Monster Class)");
    ruleConfigurationPreferences.setRuleParameter(2);
    ruleConfigurationPreferences.ok();

    new JavaPerspective().open();
    var ruleDescriptionView = new RuleDescriptionView();
    ruleDescriptionView.open();
    var onTheFlyView = new OnTheFlyView();
    onTheFlyView.open();

    var project = importExistingProjectIntoWorkspace("java/java-education-rule", "java-education-rule");

    openFileAndWaitForAnalysisCompletion(project.getResource("src", "hello", "MonsterClass.java"));

    var defaultEditor = new DefaultEditor();
    assertThat(defaultEditor.getMarkers())
      .extracting(Marker::getText, Marker::getLineNumber)
      .contains(
        tuple("Split this “Monster Class” into smaller and more specialized ones to reduce its dependencies on other classes from 3 to the maximum authorized 2 or less.", 3));

    onTheFlyView.getIssues(new MarkerDescriptionMatcher(CoreMatchers.containsString("Monster Class"))).get(0).select();
    ruleDescriptionView.open();

    new WaitUntil(new RuleDescriptionViewIsLoaded(ruleDescriptionView));
    await().untilAsserted(() -> assertThat(ruleDescriptionView.getFlatTextContent()).contains("Classes should not depend on an excessive number of classes (aka Monster Class)"));

    assertThat(ruleDescriptionView.getSections().getTabItemLabels()).containsExactly("Why is this an issue?", "How can I fix it?", "More Info");
  }

}
