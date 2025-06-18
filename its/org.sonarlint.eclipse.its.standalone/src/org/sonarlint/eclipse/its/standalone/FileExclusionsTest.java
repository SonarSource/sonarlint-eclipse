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

import org.awaitility.Awaitility;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.eclipse.jdt.ui.javaeditor.JavaEditor;
import org.eclipse.reddeer.eclipse.ui.perspectives.JavaPerspective;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.menu.ContextMenu;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.junit.After;
import org.junit.Test;
import org.sonarlint.eclipse.its.shared.AbstractSonarLintTest;
import org.sonarlint.eclipse.its.shared.reddeer.conditions.OnTheFlyViewIsEmpty;
import org.sonarlint.eclipse.its.shared.reddeer.preferences.FileExclusionsPreferences;
import org.sonarlint.eclipse.its.shared.reddeer.views.OnTheFlyView;
import org.sonarlint.eclipse.its.shared.reddeer.views.ReportView;
import org.sonarlint.eclipse.its.shared.reddeer.views.SonarLintConsole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class FileExclusionsTest extends AbstractSonarLintTest {
  @After
  public void after() {
    var console = new SonarLintConsole();
    console.enableIdeSpecificLogs(false);
  }

  @Test
  public void should_exclude_file() throws Exception {
    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("java/java-simple", "java-simple");

    var issuesView = new OnTheFlyView();
    issuesView.open();

    var helloFile = rootProject.getResource("src", "hello", "Hello.java");
    openFileAndWaitForAnalysisCompletion(helloFile);

    waitForSonarLintMarkers(issuesView,
      tuple("Replace this use of System.out by a logger.", "Hello.java", "few seconds ago"));

    new JavaEditor("Hello.java").close();

    // Exclude file
    helloFile.select();
    new ContextMenu(helloFile.getTreeItem()).getItem("SonarQube", "Exclude").select();

    // ensure issues markers are cleared even before the next analysis
    new WaitUntil(new OnTheFlyViewIsEmpty(issuesView), TimePeriod.getCustom(30));
    waitForNoSonarLintMarkers(issuesView);

    helloFile.select();
    assertThat(new ContextMenuItem("SonarQube", "Exclude").isEnabled()).isFalse();
    assertThat(new ContextMenuItem("SonarQube", "Analyze").isEnabled()).isFalse();

    // reopen the file to ensure issue doesn't come back
    openFileAndWaitForAnalysisCompletion(helloFile);
    waitForNoSonarLintMarkers(issuesView);

    var javaEditor = new JavaEditor("Hello.java");
    javaEditor.insertText(8, 29, "2");

    doAndWaitForSonarLintAnalysisJob(() -> javaEditor.save());
    waitForNoSonarLintMarkers(issuesView);

    // Trigger manual analysis of the project
    // Clear the preference when running tests locally in developer env
    ConfigurationScope.INSTANCE.getNode(UI_PLUGIN_ID).remove(PREF_SKIP_CONFIRM_ANALYZE_MULTIPLE_FILES);
    rootProject.select();
    new ContextMenu(rootProject.getTreeItem()).getItem("SonarQube", "Analyze").select();

    doAndWaitForSonarLintAnalysisJob(() -> new OkButton(shellByName("Confirmation").get()).click());
    waitForNoSonarLintMarkers(issuesView);
  }

  @Test
  public void should_add_new_entry() {
    var preferenceDialog = openPreferenceDialog();
    var fileExclusionsPreferences = new FileExclusionsPreferences(preferenceDialog);
    preferenceDialog.select(fileExclusionsPreferences);

    fileExclusionsPreferences.add("foo");
    assertThat(fileExclusionsPreferences.getExclusions()).containsOnly("foo");
    fileExclusionsPreferences.remove("foo");

    preferenceDialog.cancel();
  }

  @Test
  public void test_JDT_output_directory_equals_project_directory() {
    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("java/TestOutputEqualsProject", "TestOutputEqualsProject");

    var issuesView = new OnTheFlyView();
    issuesView.open();

    var console = new SonarLintConsole();
    console.enableIdeSpecificLogs(true);
    console.clear();

    // i) We check that the Java file was excluded because the project has no source folders configured in .classpath.
    var javaFile = rootProject.getResource("src", "aaa", "Main.java");
    javaFile.select();
    javaFile.getTreeItem().doubleClick();
    waitForNoSonarLintMarkers(issuesView);
    Awaitility.await().untilAsserted(() -> assertThat(console.getConsoleView().getConsoleText())
      .contains("File 'src/aaa/Main.java' excluded by 'JavaProjectConfiguratorExtension'"));
    new JavaEditor("Main.java").close();

    // ii) We check that the project directory was not excluded by running a Python analysis yielding one result.
    var pythonFile = rootProject.getResource("Test.py");
    openFileAndWaitForAnalysisCompletion(pythonFile);
    waitForSonarLintMarkers(issuesView,
      tuple("Complete the task associated to this \"TODO\" comment.", "Test.py", "few seconds ago"));
  }

  /**
   *  When there is a file with an unsupported charset (or encoding) in the project, it should be excluded from
   *  indexing and the analysis but should not impact other files in the project.
   */
  @Test
  public void should_exclude_file_with_unsupported_charset() {
    var reportView = new ReportView();
    reportView.open();

    var console = new SonarLintConsole();
    console.clear();
    console.enableIdeSpecificLogs(true);

    var project = importExistingProjectIntoWorkspace("UnsupportedCharset", "UnsupportedCharset");

    // i) We check that opening the file does not trigger an uncaught exception.
    var unsupportedFile = project.getResource("din_66003.xml");
    open(unsupportedFile);
    // There won't be an analysis triggered, but we have to await the "failed" text editor to load
    try {
      Thread.sleep(1000);
    } catch (Exception ignored) {
    }

    var errorPopupOpt = shellByName("Problem Occurred");
    assertThat(errorPopupOpt).isEmpty();
    // We cannot close the "failed" text editor as there is no class for it in Eclipse RedDeer.

    // ii) We check that manually forcing an analysis on the file does not trigger an uncaught exception.
    unsupportedFile.select();
    new ContextMenu(unsupportedFile.getTreeItem()).getItem("SonarQube", "Analyze").select();
    errorPopupOpt = shellByName("Problem Occurred");
    assertThat(errorPopupOpt).isEmpty();

    // iii) We check that a full project analysis does only run on the non-excluded files and yields results.
    project.select();
    new ContextMenu(project.getTreeItem()).getItem("SonarQube", "Analyze").select();
    waitForSonarLintReportIssues(reportView, 2);

    // iv) We check that everything is properly logged in the console as a IDE trace.
    assertThat(console.getConsoleView().getConsoleText())
      .contains("[SonarLintUtils#hasSupportedCharset] Unsupported charset/encoding for file:");
  }
}
