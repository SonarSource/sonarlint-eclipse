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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.core.matcher.WithLabelMatcher;
import org.eclipse.reddeer.eclipse.core.resources.Project;
import org.eclipse.reddeer.eclipse.core.resources.Resource;
import org.eclipse.reddeer.eclipse.ui.dialogs.PropertyDialog;
import org.eclipse.reddeer.eclipse.ui.markers.AbstractMarker;
import org.eclipse.reddeer.eclipse.ui.markers.matcher.MarkerDescriptionMatcher;
import org.eclipse.reddeer.eclipse.ui.perspectives.JavaPerspective;
import org.eclipse.reddeer.eclipse.ui.views.markers.ProblemsView;
import org.eclipse.reddeer.eclipse.ui.views.markers.ProblemsView.ProblemType;
import org.eclipse.reddeer.eclipse.ui.views.markers.QuickFixWizard;
import org.eclipse.reddeer.jface.text.contentassist.ContentAssistant;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.table.DefaultTable;
import org.eclipse.reddeer.swt.impl.text.DefaultText;
import org.eclipse.reddeer.workbench.condition.TextEditorContainsText;
import org.eclipse.reddeer.workbench.impl.editor.TextEditor;
import org.eclipse.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;
import org.junit.After;
import org.junit.Test;
import org.sonarlint.eclipse.its.reddeer.preferences.SonarLintPreferences;
import org.sonarlint.eclipse.its.reddeer.preferences.SonarLintPreferences.MarkerSeverity;
import org.sonarlint.eclipse.its.reddeer.views.OnTheFlyView;

import static org.assertj.core.api.Assertions.assertThat;

public class QuickFixesTest extends AbstractSonarLintTest {
  private static final String ISSUE_MESSAGE = "Remove this unnecessary cast to \"int\".";
  private static final String QUICK_FIX_MESSAGE = "Remove the cast to \"int\"";
  private static final String EXPECTED_TEXT = "return hashCode();";
  private static final String FILE_NAME = "Hello.java";
  private static final String PLACEHOLDER_LICENSE_HEADER = "/**\nPlaceholder\nlicense\nheader\n*/";

  @After
  public void tearDown() {
    // default value
    setMarkerSeverityToWarning(MarkerSeverity.INFO);
  }

  @Test
  public void shouldApplyQuickFixThroughContentAssist() {
    importAndOpenQuickFixableFile();

    TextEditor editor = new TextEditor(FILE_NAME);
    editor.setCursorPosition(8, 14);
    // uses the keyboard and may not work on non-US layouts
    ContentAssistant assistant = editor.openQuickFixContentAssistant();
    assertThat(assistant.getProposals()).element(0).isEqualTo(QUICK_FIX_MESSAGE);
    assistant.chooseProposal(QUICK_FIX_MESSAGE);

    new WaitUntil(new TextEditorContainsText(editor, EXPECTED_TEXT));
  }

  @Test
  public void shouldApplyQuickFixThroughMarkerContextMenu() {
    // default issue severity is INFO and reddeer supports only ERROR and WARNING
    setMarkerSeverityToWarning(MarkerSeverity.WARNING);
    importAndOpenQuickFixableFile();

    TextEditor editor = new TextEditor(FILE_NAME);
    editor.activate();
    applyFixThrough(findQuickFixableMarkerInProblemsView().openQuickFix());

    new WaitUntil(new TextEditorContainsText(editor, EXPECTED_TEXT));
  }

  @Test
  public void shouldApplyQuickFixThroughOnTheFlyViewContextMenu() {
    importAndOpenQuickFixableFile();

    TextEditor editor = new TextEditor(FILE_NAME);
    editor.activate();
    new OnTheFlyView().getItems().get(0).select();
    new ContextMenuItem("Quick Fix").select();
    applyFixThrough(new QuickFixWizard());

    new WaitUntil(new TextEditorContainsText(editor, EXPECTED_TEXT));
  }

  @Test
  public void shouldApplyQuickFixOnClosedFileThroughMarkerContextMenu() {
    // default issue severity is INFO and reddeer supports only ERROR and WARNING
    setMarkerSeverityToWarning(MarkerSeverity.WARNING);
    importAndOpenQuickFixableFile();
    new TextEditor(FILE_NAME).close();

    applyFixThrough(findQuickFixableMarkerInProblemsView().openQuickFix());

    new WaitUntil(new TextEditorContainsText(new TextEditor(FILE_NAME), EXPECTED_TEXT));
  }

  @Test
  public void shouldApplyQuickFixInDirtyFile() {
    // default issue severity is INFO and reddeer supports only ERROR and WARNING
    setMarkerSeverityToWarning(MarkerSeverity.WARNING);
    importAndOpenQuickFixableFile();
    TextEditor editor = new TextEditor(FILE_NAME);
    editor.insertText(0, PLACEHOLDER_LICENSE_HEADER);

    applyFixThrough(findQuickFixableMarkerInProblemsView().openQuickFix());

    new WaitUntil(new TextEditorContainsText(editor, EXPECTED_TEXT));
  }

  @Test
  public void shouldApplyQuickFixAfterFileModifiedOnFileSystem() throws IOException {
    // default issue severity is INFO and reddeer supports only ERROR and WARNING
    setMarkerSeverityToWarning(MarkerSeverity.WARNING);
    Resource file = importAndOpenQuickFixableFile();
    TextEditor editor = new TextEditor(FILE_NAME);
    Files.write(getFilePath(file), (PLACEHOLDER_LICENSE_HEADER + editor.getText()).getBytes());
    editor.activate();

    applyFixThrough(findQuickFixableMarkerInProblemsView().openQuickFix());

    new WaitUntil(new TextEditorContainsText(editor, EXPECTED_TEXT));
  }

  @Test
  public void shouldNotProposeQuickFixWhenTargetRangeIsInvalid() {
    // default issue severity is INFO and reddeer supports only ERROR and WARNING
    setMarkerSeverityToWarning(MarkerSeverity.WARNING);
    importAndOpenQuickFixableFile();
    new TextEditor(FILE_NAME).insertText(8, 14, "random change");

    QuickFixWizard quickFixWizard = findQuickFixableMarkerInProblemsView().openQuickFix();
    boolean quickFixProposed = new DefaultTable(quickFixWizard, 0).containsItem(QUICK_FIX_MESSAGE);
    quickFixWizard.cancel();

    assertThat(quickFixProposed).isFalse();
  }

  private Resource importAndOpenQuickFixableFile() {
    new JavaPerspective().open();
    Project rootProject = importExistingProjectIntoWorkspace("java/quick-fixes", "quick-fixes");
    Resource file = rootProject.getResource("src", "hello", FILE_NAME);
    openFileAndWaitForAnalysisCompletion(file);
    return file;
  }

  private static void applyFixThrough(QuickFixWizard wizard) {
    new DefaultTable(wizard, 0).select(QUICK_FIX_MESSAGE);
    new PushButton(wizard, "Select All").click();
    wizard.finish();
  }

  private static void setMarkerSeverityToWarning(MarkerSeverity severity) {
    WorkbenchPreferenceDialog preferenceDialog = new WorkbenchPreferenceDialog();
    preferenceDialog.open();
    SonarLintPreferences preferences = new SonarLintPreferences(preferenceDialog);
    preferenceDialog.select(preferences);
    preferences.setMarkersSeverity(severity);
    preferenceDialog.ok();
  }

  private static Path getFilePath(Resource resource) {
    PropertyDialog properties = resource.openProperties();
    String filePath = new DefaultText(properties, new WithLabelMatcher("Location:")).getText();
    properties.cancel();
    return Paths.get(filePath);
  }

  private static AbstractMarker findQuickFixableMarkerInProblemsView() {
    return new ProblemsView().getProblems(ProblemType.ALL, new MarkerDescriptionMatcher(ISSUE_MESSAGE)).get(0);
  }

}
