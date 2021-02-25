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
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.eclipse.core.resources.Project;
import org.eclipse.reddeer.eclipse.core.resources.Resource;
import org.eclipse.reddeer.eclipse.jdt.ui.javaeditor.JavaEditor;
import org.eclipse.reddeer.eclipse.jdt.ui.packageview.PackageExplorerPart;
import org.eclipse.reddeer.eclipse.ui.perspectives.JavaPerspective;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.menu.ContextMenu;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;
import org.junit.Test;
import org.sonarlint.eclipse.its.reddeer.conditions.OnTheFlyViewIsEmpty;
import org.sonarlint.eclipse.its.reddeer.preferences.FileExclusionsPreferences;
import org.sonarlint.eclipse.its.reddeer.views.OnTheFlyView;
import org.sonarlint.eclipse.its.reddeer.views.SonarLintIssue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class FileExclusionsTest extends AbstractSonarLintTest {

  @Test
  public void should_exclude_file() throws Exception {
    new JavaPerspective().open();
    importExistingProjectIntoWorkspace("java/java-simple");

    OnTheFlyView issuesView = new OnTheFlyView();
    issuesView.open();
    
    PackageExplorerPart packageExplorer = new PackageExplorerPart();
    Project rootProject = packageExplorer.getProject("java-simple");
    Resource helloFile = rootProject.getResource("src", "hello", "Hello.java");
    helloFile.open();

    waitForSonarLintJob();

    List<SonarLintIssue> sonarlintIssues = issuesView.getIssues();

    assertThat(sonarlintIssues).extracting(SonarLintIssue::getResource, SonarLintIssue::getDescription)
      .containsOnly(tuple("Hello.java", "Replace this use of System.out or System.err by a logger."));

    new JavaEditor("Hello.java").close();

    // Exclude file
    helloFile.select();
    new ContextMenu(helloFile.getTreeItem()).getItem("SonarLint", "Exclude").select();

    // ensure issues markers are cleared even before the next analysis
    new WaitUntil(new OnTheFlyViewIsEmpty(issuesView), TimePeriod.MEDIUM);
    assertThat(issuesView.getIssues()).isEmpty();

    helloFile.select();
    assertThat(new ContextMenuItem("SonarLint", "Exclude").isEnabled()).isFalse();
    assertThat(new ContextMenuItem("SonarLint", "Analyze").isEnabled()).isFalse();

    // reopen the file to ensure issue doesn't come back
    helloFile.open();

    waitForSonarLintJob();

    assertThat(issuesView.getIssues()).isEmpty();

    JavaEditor javaEditor = new JavaEditor("Hello.java");
    javaEditor.insertText(8, 29, "2");
    javaEditor.save();

    waitForSonarLintJob();

    assertThat(issuesView.getIssues()).isEmpty();

    // Trigger manual analysis of the project
    // Clear the preference when running tests locally in developper env
    ConfigurationScope.INSTANCE.getNode(UI_PLUGIN_ID).remove(PREF_SKIP_CONFIRM_ANALYZE_MULTIPLE_FILES);
    rootProject.select();
    new ContextMenu(rootProject.getTreeItem()).getItem("SonarLint", "Analyze").select();
    new OkButton().click();

    waitForSonarLintJob();

    assertThat(issuesView.getIssues()).isEmpty();
  }

  @Test
  public void should_add_new_entry() {
    WorkbenchPreferenceDialog preferenceDialog = new WorkbenchPreferenceDialog();
    preferenceDialog.open();

    FileExclusionsPreferences fileExclusionsPreferences = new FileExclusionsPreferences(preferenceDialog);
    preferenceDialog.select(fileExclusionsPreferences);

    fileExclusionsPreferences.add("foo");
    assertThat(fileExclusionsPreferences.getExclusions()).containsOnly("foo");
    fileExclusionsPreferences.remove("foo");

    preferenceDialog.cancel();
  }

}
