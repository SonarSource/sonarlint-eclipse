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

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.eclipse.jdt.ui.javaeditor.JavaEditor;
import org.eclipse.reddeer.eclipse.ui.perspectives.JavaPerspective;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.menu.ContextMenu;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;
import org.junit.Test;
import org.sonarlint.eclipse.its.shared.AbstractSonarLintTest;
import org.sonarlint.eclipse.its.shared.reddeer.conditions.OnTheFlyViewIsEmpty;
import org.sonarlint.eclipse.its.shared.reddeer.preferences.FileExclusionsPreferences;
import org.sonarlint.eclipse.its.shared.reddeer.views.OnTheFlyView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class FileExclusionsTest extends AbstractSonarLintTest {

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
    var preferenceDialog = new WorkbenchPreferenceDialog();
    preferenceDialog.open();

    var fileExclusionsPreferences = new FileExclusionsPreferences(preferenceDialog);
    preferenceDialog.select(fileExclusionsPreferences);

    fileExclusionsPreferences.add("foo");
    assertThat(fileExclusionsPreferences.getExclusions()).containsOnly("foo");
    fileExclusionsPreferences.remove("foo");

    preferenceDialog.cancel();
  }

}
