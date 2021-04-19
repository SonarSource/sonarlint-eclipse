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

import org.eclipse.reddeer.eclipse.core.resources.Project;
import org.eclipse.reddeer.eclipse.ui.perspectives.JavaPerspective;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.impl.link.DefaultLink;
import org.eclipse.reddeer.swt.impl.menu.ContextMenu;
import org.eclipse.reddeer.workbench.impl.editor.DefaultEditor;
import org.eclipse.reddeer.workbench.impl.editor.Marker;
import org.eclipse.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;
import org.junit.Test;
import org.sonarlint.eclipse.its.reddeer.preferences.RuleConfigurationPreferences;
import org.sonarlint.eclipse.its.reddeer.views.OnTheFlyView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assume.assumeTrue;

public class RulesConfigurationTest extends AbstractSonarLintTest {

  @Test
  public void deactivate_rule() {
    new JavaPerspective().open();
    Project rootProject = importExistingProjectIntoWorkspace("java/java-exclude-rules","java-exclude-rules");

    OnTheFlyView onTheFlyView = new OnTheFlyView();
    onTheFlyView.open();

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "hello", "Hello3.java"));

    checkIssueIsDefault();

    doAndWaitForSonarLintAnalysisJob(() -> new ContextMenu(onTheFlyView.getItems().get(1)).getItem("Deactivate rule").select());

    DefaultEditor defaultEditor = new DefaultEditor();
    assertThat(defaultEditor.getMarkers())
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsExactly(tuple("Replace this use of System.out or System.err by a logger.", 9));

    doAndWaitForSonarLintAnalysisJob(() -> restoreDefaultRulesConfiguration());

    checkIssueIsDefault();
    doAndWaitForSonarLintAnalysisJob(() -> lowerCognitiveComplexityRuleParameter());
    checkIssueChanged();

    doAndWaitForSonarLintAnalysisJob(() -> restoreDefaultRulesConfiguration());
    checkIssueIsDefault();
  }

  @Test
  public void ruleParametersGlobalDefaults() {
    new JavaPerspective().open();
    Project rootProject = importExistingProjectIntoWorkspace("java/java-exclude-rules", "java-exclude-rules");

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "hello", "Hello3.java"));

    checkIssueIsDefault();
    doAndWaitForSonarLintAnalysisJob(() -> lowerCognitiveComplexityRuleParameter());
    checkIssueChanged();

    doAndWaitForSonarLintAnalysisJob(() -> restoreDefaultRulesConfiguration());

    checkIssueIsDefault();
  }

  @Test
  public void ruleParametersActivationRoundTrip() {
    RuleConfigurationPreferences ruleConfigurationPreferences = openRuleConfigurationPreferences();
    TreeItem cognitiveComplexityRuleItem = getCognitiveComplexityRuleTreeItem(ruleConfigurationPreferences);
    cognitiveComplexityRuleItem.select();

    assertThat(ruleConfigurationPreferences.getRuleParamSpinner().isEnabled()).isTrue();

    new ContextMenu(cognitiveComplexityRuleItem).getItem("Deactivate").select();
    assertThat(ruleConfigurationPreferences.getRuleParamSpinner().isEnabled()).isFalse();

    new ContextMenu(cognitiveComplexityRuleItem).getItem("Activate").select();
    assertThat(ruleConfigurationPreferences.getRuleParamSpinner().isEnabled()).isTrue();
  }

  @Test
  public void defaultLinkVisibilityRoundTrip() {
    RuleConfigurationPreferences ruleConfigurationPreferences = selectCognitiveComplexityRule();

    assertThat(paramRestoreDefaultLink()).isNull();

    ruleConfigurationPreferences.setRuleParameter(10);
    assertThat(paramRestoreDefaultLink()).isNotNull();
    ruleConfigurationPreferences.setRuleParameter(15);
    assertThat(paramRestoreDefaultLink()).isNull();
  }

  @Test
  public void open_rules_configuration() {
    assumeTrue(isMarsOrGreater());

    RuleConfigurationPreferences ruleConfigurationPreferences = openRuleConfigurationPreferences();

    assertThat(ruleConfigurationPreferences.getItems()).hasSize(5 /* HTML, Java, JavaScript, PHP, Python - no TypeScript */);
    TreeItem htmlNode = ruleConfigurationPreferences.getItems().get(0);

    assertThat(htmlNode.getText()).isEqualTo("HTML");

    htmlNode.expand();
    assertThat(htmlNode.getItems().get(0).getText()).isEqualTo("\"<!DOCTYPE>\" declarations should appear before \"<html>\" tags");
  }

  private static RuleConfigurationPreferences openRuleConfigurationPreferences() {
    WorkbenchPreferenceDialog preferenceDialog = new WorkbenchPreferenceDialog();
    preferenceDialog.open();

    RuleConfigurationPreferences ruleConfigurationPreferences = new RuleConfigurationPreferences(preferenceDialog);
    preferenceDialog.select(ruleConfigurationPreferences);
    return ruleConfigurationPreferences;
  }

  private DefaultLink paramRestoreDefaultLink() {
    try {
      return new DefaultLink("Restore defaults");
    } catch (Exception e) {
      return null;
    }
  }

  void restoreDefaultRulesConfiguration() {
    WorkbenchPreferenceDialog preferenceDialog = new WorkbenchPreferenceDialog();
    preferenceDialog.open();

    RuleConfigurationPreferences ruleConfigurationPreferences = new RuleConfigurationPreferences(preferenceDialog);
    preferenceDialog.select(ruleConfigurationPreferences);
    ruleConfigurationPreferences.restoreDefaults();
    preferenceDialog.ok();
  }

  private static void lowerCognitiveComplexityRuleParameter() {
    RuleConfigurationPreferences ruleConfigurationPreferences = selectCognitiveComplexityRule();
    ruleConfigurationPreferences.setRuleParameter(10);
    ruleConfigurationPreferences.ok();
  }

  private static RuleConfigurationPreferences selectCognitiveComplexityRule() {
    RuleConfigurationPreferences ruleConfigurationPreferences = openRuleConfigurationPreferences();
    getCognitiveComplexityRuleTreeItem(ruleConfigurationPreferences).select();
    return ruleConfigurationPreferences;
  }

  private static TreeItem getCognitiveComplexityRuleTreeItem(RuleConfigurationPreferences ruleConfigurationPreferences) {
    ruleConfigurationPreferences.filter("java:S3776");
    return ruleConfigurationPreferences.getItem("Java", "Cognitive Complexity of methods should not be too high");
  }

  static void checkIssueChanged() {
    DefaultEditor defaultEditor = new DefaultEditor();
    assertThat(defaultEditor.getMarkers())
      .filteredOn(m -> "org.sonarlint.eclipse.onTheFlyIssueAnnotationType".equals(m.getType()))
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsOnly(
        tuple("Replace this use of System.out or System.err by a logger.", 9),
        tuple("Refactor this method to reduce its Cognitive Complexity from 24 to the 10 allowed.", 12));
  }

  void checkIssueIsDefault() {
    DefaultEditor defaultEditor = new DefaultEditor();
    assertThat(defaultEditor.getMarkers())
      .filteredOn(m -> "org.sonarlint.eclipse.onTheFlyIssueAnnotationType".equals(m.getType()))
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsOnly(
        tuple("Replace this use of System.out or System.err by a logger.", 9),
        tuple("Refactor this method to reduce its Cognitive Complexity from 24 to the 15 allowed.", 12));
  }

}
