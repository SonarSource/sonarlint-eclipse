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

import org.eclipse.reddeer.eclipse.ui.dialogs.PropertyDialog;
import org.eclipse.reddeer.eclipse.ui.perspectives.JavaPerspective;
import org.junit.After;
import org.junit.Test;
import org.sonarlint.eclipse.its.shared.AbstractSonarLintTest;
import org.sonarlint.eclipse.its.shared.reddeer.preferences.AnalyzerPropertiesPreferences;
import org.sonarlint.eclipse.its.shared.reddeer.views.OnTheFlyView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class AnalyzerPropertiesTest extends AbstractSonarLintTest {
  @After
  public void clearGlobalAnalyzerProperties() {
    var preferenceDialog = openPreferenceDialog();
    var analyzerPropertiesPreferences = new AnalyzerPropertiesPreferences(preferenceDialog);
    preferenceDialog.select(analyzerPropertiesPreferences);

    analyzerPropertiesPreferences.clear();
    preferenceDialog.ok();
  }

  @Test
  public void test_analyzer_properties_trigger_analysis() {
    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("java/java-simple", "java-simple");

    var issuesView = new OnTheFlyView();
    issuesView.open();

    var helloFile = rootProject.getResource("src", "hello", "Hello.java");
    openFileAndWaitForAnalysisCompletion(helloFile);

    waitForSonarLintMarkers(issuesView,
      tuple("Replace this use of System.out by a logger.", "Hello.java", "few seconds ago"));

    // i) Change global properties
    var preferenceDialog = openPreferenceDialog();
    var analyzerPropertiesPreferences = new AnalyzerPropertiesPreferences(preferenceDialog);
    preferenceDialog.select(analyzerPropertiesPreferences);

    analyzerPropertiesPreferences.add("sonar.java.fileByFile", "true");
    assertThat(analyzerPropertiesPreferences.getProperties().size()).isEqualTo(1);
    doAndWaitForSonarLintAnalysisJob(preferenceDialog::ok);

    // ii) Change project properties
    rootProject.select();
    var projectPreferenceDialog = new PropertyDialog(rootProject.getName());
    projectPreferenceDialog.open();

    analyzerPropertiesPreferences = new AnalyzerPropertiesPreferences(projectPreferenceDialog);
    projectPreferenceDialog.select(analyzerPropertiesPreferences);

    analyzerPropertiesPreferences.add("sonar.java.fileByFile", "false");
    assertThat(analyzerPropertiesPreferences.getProperties().size()).isEqualTo(1);
    doAndWaitForSonarLintAnalysisJob(projectPreferenceDialog::ok);
  }
}
