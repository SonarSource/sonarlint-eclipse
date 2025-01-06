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

import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.eclipse.ui.markers.matcher.MarkerDescriptionMatcher;
import org.eclipse.reddeer.eclipse.ui.perspectives.JavaPerspective;
import org.eclipse.reddeer.workbench.impl.editor.DefaultEditor;
import org.hamcrest.CoreMatchers;
import org.junit.Assume;
import org.junit.Test;
import org.sonarlint.eclipse.its.shared.AbstractSonarLintTest;
import org.sonarlint.eclipse.its.shared.reddeer.conditions.RuleDescriptionViewIsLoaded;
import org.sonarlint.eclipse.its.shared.reddeer.preferences.RuleConfigurationPreferences;
import org.sonarlint.eclipse.its.shared.reddeer.views.OnTheFlyView;
import org.sonarlint.eclipse.its.shared.reddeer.views.RuleDescriptionView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

public class RuleDescriptionViewTest extends AbstractSonarLintTest {

  @Test
  public void openRuleDescription() {
    // Because the CI can only provide GTK 4+ WebKit libraries, Eclipse 4.8 requires GTK3 tho!
    Assume.assumeTrue(!"oldest-java-11_e48".equals(System.getProperty("target.platform")));

    new JavaPerspective().open();
    var ruleDescriptionView = new RuleDescriptionView();
    ruleDescriptionView.open();
    var onTheFlyView = new OnTheFlyView();
    onTheFlyView.open();

    var project = importExistingProjectIntoWorkspace("java/java-simple", "java-simple");

    openFileAndWaitForAnalysisCompletion(project.getResource("src", "hello", "Hello.java"));
    waitForMarkers(new DefaultEditor(),
      tuple("Replace this use of System.out by a logger.", 9));

    onTheFlyView.selectItem(0);
    ruleDescriptionView.open();

    new WaitUntil(new RuleDescriptionViewIsLoaded(ruleDescriptionView));
    await().untilAsserted(() -> assertThat(ruleDescriptionView.getFlatTextContent())
      .contains("java:S106"));
  }

  @Test
  public void openRuleDescription_with_educational_content() {
    // Because the CI can only provide GTK 4+ WebKit libraries, Eclipse 4.8 requires GTK3 tho!
    Assume.assumeTrue(!"oldest-java-11_e48".equals(System.getProperty("target.platform")));

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
    waitForMarkers(new DefaultEditor(),
      tuple("Remove this unused \"dep2\" private field.", 6),
      tuple("Remove this unused \"dep1\" private field.", 5),
      tuple("Remove this unused \"dep3\" private field.", 7),
      tuple("Split this “Monster Class” into smaller and more specialized ones to reduce its dependencies on other classes from 3 to the maximum authorized 2 or less.", 3));

    // INFO: It is okey to not wait for the SonarLint markers here as it was done above for the markers in the editor!
    onTheFlyView.getIssues(new MarkerDescriptionMatcher(CoreMatchers.containsString("Monster Class"))).get(0).select();
    ruleDescriptionView.open();

    new WaitUntil(new RuleDescriptionViewIsLoaded(ruleDescriptionView));
    await().untilAsserted(() -> assertThat(ruleDescriptionView.getFlatTextContent()).contains("Classes should not depend on an excessive number of classes (aka Monster Class)"));

    assertThat(ruleDescriptionView.getSections().getTabItemLabels()).containsExactly("Why is this an issue?", "How can I fix it?", "More Info");
  }

  /**
   *  SLE-636: For testing the Syntax Highlighting in Python we have to open the Rule Description view to trigger the
   *           extension point on the specific (sub-)plug-in!
   */
  @Test
  public void openRuleRescription_with_PythonSyntaxHighlighting() {
    // Because the CI can only provide GTK 4+ WebKit libraries, Eclipse 4.8 requires GTK3 tho!
    Assume.assumeTrue(!"oldest-java-11_e48".equals(System.getProperty("target.platform")));

    new JavaPerspective().open();
    var ruleDescriptionView = new RuleDescriptionView();
    ruleDescriptionView.open();
    var onTheFlyView = new OnTheFlyView();
    onTheFlyView.open();

    var project = importExistingProjectIntoWorkspace("python", "python");

    openFileAndWaitForAnalysisCompletion(project.getResource("src", "root", "nested", "example.py"));
    waitForMarkers(new DefaultEditor(),
      tuple("Merge this if statement with the enclosing one.", 9),
      tuple("Replace \"<>\" by \"!=\".", 9),
      tuple("Replace print statement by built-in function.", 10));

    onTheFlyView.selectFirstItemWithDescription("Replace print statement by built-in function.");
    ruleDescriptionView.open();

    new WaitUntil(new RuleDescriptionViewIsLoaded(ruleDescriptionView));
    await().untilAsserted(() -> assertThat(ruleDescriptionView.getFlatTextContent())
      .contains("python:PrintStatementUsage"));
  }
}
