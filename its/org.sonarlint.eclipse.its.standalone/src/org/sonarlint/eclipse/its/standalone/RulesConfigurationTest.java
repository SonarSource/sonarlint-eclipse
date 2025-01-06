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

import org.eclipse.reddeer.eclipse.ui.markers.matcher.MarkerDescriptionMatcher;
import org.eclipse.reddeer.eclipse.ui.perspectives.JavaPerspective;
import org.eclipse.reddeer.swt.impl.link.DefaultLink;
import org.eclipse.reddeer.swt.impl.menu.ContextMenu;
import org.eclipse.reddeer.workbench.impl.editor.DefaultEditor;
import org.eclipse.reddeer.workbench.impl.editor.Marker;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.sonarlint.eclipse.its.shared.AbstractSonarLintTest;
import org.sonarlint.eclipse.its.shared.reddeer.preferences.RuleConfigurationPreferences;
import org.sonarlint.eclipse.its.shared.reddeer.views.OnTheFlyView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

public class RulesConfigurationTest extends AbstractSonarLintTest {
  @Test
  public void deactivate_rule() {
    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("java/java-exclude-rules", "java-exclude-rules");

    var onTheFlyView = new OnTheFlyView();
    onTheFlyView.open();

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "hello", "Hello3.java"));
    checkIssueIsDefault();
    new DefaultEditor().close();

    var cognitiveComplexityMarkerMatcher = new MarkerDescriptionMatcher(
      CoreMatchers.containsString("Refactor this method to reduce its Cognitive Complexity from 24 to the 15 allowed."));
    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "hello", "Hello3.java"));
    doAndWaitForSonarLintAnalysisJob(() -> onTheFlyView.getIssues(cognitiveComplexityMarkerMatcher).get(0).deactivateRule());
    var defaultEditor = new DefaultEditor();
    await().untilAsserted(() -> assertThat(defaultEditor.getMarkers())
      .filteredOn(m -> ON_THE_FLY_ANNOTATION_TYPE.equals(m.getType()))
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsExactly(tuple("Replace this use of System.out by a logger.", 9)));
    new DefaultEditor().close();

    restoreDefaultRulesConfiguration();
    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "hello", "Hello3.java"));
    checkIssueIsDefault();
    new DefaultEditor().close();

    lowerCognitiveComplexityRuleParameter();
    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "hello", "Hello3.java"));
    checkIssueChanged();
    new DefaultEditor().close();

    restoreDefaultRulesConfiguration();
    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "hello", "Hello3.java"));
    checkIssueIsDefault();
  }

  @Test
  public void ruleParametersGlobalDefaults() {
    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("java/java-exclude-rules", "java-exclude-rules");

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "hello", "Hello3.java"));
    checkIssueIsDefault();
    new DefaultEditor().close();

    lowerCognitiveComplexityRuleParameter();
    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "hello", "Hello3.java"));
    checkIssueChanged();
    new DefaultEditor().close();

    restoreDefaultRulesConfiguration();
    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "hello", "Hello3.java"));
    checkIssueIsDefault();
  }

  @Test
  public void ruleParametersActivationRoundTrip() {
    var ruleConfigurationPreferences = RuleConfigurationPreferences.open();
    var cognitiveComplexityRuleItem = ruleConfigurationPreferences.selectRule("java:S3776", "Java", "Cognitive Complexity of methods should not be too high");
    await().untilAsserted(() -> assertThat(ruleConfigurationPreferences.getRuleParamSpinner().isEnabled()).isTrue());

    new ContextMenu(cognitiveComplexityRuleItem).getItem("Deactivate").select();
    await().untilAsserted(() -> assertThat(ruleConfigurationPreferences.getRuleParamSpinner().isEnabled()).isFalse());

    new ContextMenu(cognitiveComplexityRuleItem).getItem("Activate").select();
    await().untilAsserted(() -> assertThat(ruleConfigurationPreferences.getRuleParamSpinner().isEnabled()).isTrue());

    ruleConfigurationPreferences.cancel();
  }

  @Test
  public void defaultLinkVisibilityRoundTrip() {
    var ruleConfigurationPreferences = selectCognitiveComplexityRule();

    assertThat(paramRestoreDefaultLink()).isNull();

    ruleConfigurationPreferences.setRuleParameter(10);
    assertThat(paramRestoreDefaultLink()).isNotNull();
    ruleConfigurationPreferences.setRuleParameter(15);
    assertThat(paramRestoreDefaultLink()).isNull();

    ruleConfigurationPreferences.cancel();
  }

  @Test
  public void open_rules_configuration() {
    var ruleConfigurationPreferences = RuleConfigurationPreferences.open();

    /* C, C++, CSS, HTML, Java, JavaScript, PHP, Python, Secrets, TypeScript, XML */
    assertThat(ruleConfigurationPreferences.getItems()).hasSize(11);
    var cssNode = ruleConfigurationPreferences.getItems().get(2);

    assertThat(cssNode.getText()).isEqualTo("CSS");

    cssNode.expand();
    assertThat(cssNode.getItems().get(0).getText()).isEqualTo("\"!important\" should not be used on \"keyframes\"");

    ruleConfigurationPreferences.cancel();
  }

  private DefaultLink paramRestoreDefaultLink() {
    try {
      return new DefaultLink("Restore defaults");
    } catch (Exception e) {
      return null;
    }
  }

  private static void lowerCognitiveComplexityRuleParameter() {
    var ruleConfigurationPreferences = selectCognitiveComplexityRule();
    ruleConfigurationPreferences.setRuleParameter(10);
    ruleConfigurationPreferences.ok();
  }

  private static RuleConfigurationPreferences selectCognitiveComplexityRule() {
    var ruleConfigurationPreferences = RuleConfigurationPreferences.open();
    ruleConfigurationPreferences.selectRule("java:S3776", "Java", "Cognitive Complexity of methods should not be too high");
    return ruleConfigurationPreferences;
  }

  static void checkIssueChanged() {
    var defaultEditor = new DefaultEditor();
    await().untilAsserted(() -> assertThat(defaultEditor.getMarkers())
      .filteredOn(m -> "org.sonarlint.eclipse.onTheFlyIssueAnnotationType".equals(m.getType()))
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsOnly(
        tuple("Replace this use of System.out by a logger.", 9),
        tuple("Refactor this method to reduce its Cognitive Complexity from 24 to the 10 allowed.", 12)));
  }

  void checkIssueIsDefault() {
    var defaultEditor = new DefaultEditor();
    await().untilAsserted(() -> assertThat(defaultEditor.getMarkers())
      .filteredOn(m -> "org.sonarlint.eclipse.onTheFlyIssueAnnotationType".equals(m.getType()))
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsOnly(
        tuple("Replace this use of System.out by a logger.", 9),
        tuple("Refactor this method to reduce its Cognitive Complexity from 24 to the 15 allowed.", 12)));
  }

}
