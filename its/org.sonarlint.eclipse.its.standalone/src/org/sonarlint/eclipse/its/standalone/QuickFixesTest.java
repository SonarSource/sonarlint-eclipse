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

import java.io.IOException;
import java.nio.file.Files;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.reddeer.common.exception.RedDeerException;
import org.eclipse.reddeer.common.wait.AbstractWait;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.core.lookup.ShellLookup;
import org.eclipse.reddeer.eclipse.condition.ProblemExists;
import org.eclipse.reddeer.eclipse.core.resources.Project;
import org.eclipse.reddeer.eclipse.core.resources.Resource;
import org.eclipse.reddeer.eclipse.ui.markers.AbstractMarker;
import org.eclipse.reddeer.eclipse.ui.markers.matcher.AbstractMarkerMatcher;
import org.eclipse.reddeer.eclipse.ui.markers.matcher.MarkerDescriptionMatcher;
import org.eclipse.reddeer.eclipse.ui.markers.matcher.MarkerTypeMatcher;
import org.eclipse.reddeer.eclipse.ui.perspectives.JavaPerspective;
import org.eclipse.reddeer.eclipse.ui.views.markers.ProblemsView;
import org.eclipse.reddeer.eclipse.ui.views.markers.ProblemsView.ProblemType;
import org.eclipse.reddeer.eclipse.ui.views.markers.QuickFixWizard;
import org.eclipse.reddeer.jface.text.contentassist.ContentAssistant;
import org.eclipse.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.menu.ShellMenuItem;
import org.eclipse.reddeer.swt.impl.table.DefaultTable;
import org.eclipse.reddeer.workbench.condition.ContentAssistantShellIsOpened;
import org.eclipse.reddeer.workbench.condition.TextEditorContainsText;
import org.eclipse.reddeer.workbench.exception.WorkbenchLayerException;
import org.eclipse.reddeer.workbench.impl.editor.DefaultEditor;
import org.eclipse.reddeer.workbench.impl.editor.TextEditor;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonarlint.eclipse.its.shared.AbstractSonarLintTest;
import org.sonarlint.eclipse.its.shared.reddeer.preferences.SonarLintPreferences;
import org.sonarlint.eclipse.its.shared.reddeer.preferences.SonarLintPreferences.MarkerSeverity;
import org.sonarlint.eclipse.its.shared.reddeer.views.OnTheFlyView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@OpenPerspective(JavaPerspective.class)
public class QuickFixesTest extends AbstractSonarLintTest {
  private static final String QUICK_FIXES_PROJECT_NAME = "quick-fixes";
  private static final String ISSUE_MESSAGE = "Remove this unnecessary cast to \"int\".";
  private static final String QUICK_FIX_MESSAGE = "Remove the cast to \"int\"";
  private static final String EXPECTED_TEXT = "return hashCode();";
  private static final String FILE_NAME = "Hello.java";
  private static final String PLACEHOLDER_LICENSE_HEADER = "/**\nPlaceholder\nlicense\nheader\n*/";
  private Project rootProject;

  @BeforeClass
  public static void init() {
    // default issue severity is INFO and reddeer supports only ERROR and WARNING
    changeSonarLintMarkerSeverity(MarkerSeverity.WARNING);
  }

  @AfterClass
  public static void restore() {
    // restore default value
    changeSonarLintMarkerSeverity(MarkerSeverity.INFO);
  }

  @Before
  public void importProject() {
    rootProject = importExistingProjectIntoWorkspace("java/quick-fixes", QUICK_FIXES_PROJECT_NAME);
  }

  @Test
  public void shouldApplyQuickFixThroughContentAssist() {
    openQuickFixableFile();
    waitForMarkers(new DefaultEditor(),
      tuple("Remove this unnecessary cast to \"int\".", 9));

    var editor = new TextEditor(FILE_NAME);
    editor.setCursorPosition(8, 14);
    var assistant = openQuickFixContentAssistant(editor);
    assertThat(assistant.getProposals()).element(0).isEqualTo(QUICK_FIX_MESSAGE);
    assistant.chooseProposal(QUICK_FIX_MESSAGE);

    new WaitUntil(new TextEditorContainsText(editor, EXPECTED_TEXT));
  }

  private ContentAssistant openQuickFixContentAssistant(TextEditor editor) {
    editor.activate();
    AbstractWait.sleep(TimePeriod.SHORT);

    ShellMenuItem menu;
    try {
      menu = new ShellMenuItem("Edit", "Quick Fix");
    } catch (RedDeerException e) {
      // log.info("Quick fix menu not found, open via keyboard shortcut");
      return editor.openQuickFixContentAssistant();

    }
    if (!menu.isEnabled()) {
      throw new WorkbenchLayerException("Quick Fix is disabled!");
    }
    var shells = ShellLookup.getInstance().getShells();
    menu.select();
    var caw = new ContentAssistantShellIsOpened(shells);
    new WaitUntil(caw);
    return new ContentAssistant(caw.getContentAssistTable());
  }

  @Test
  public void shouldApplyQuickFixThroughMarkerContextMenu() {
    openQuickFixableFile();
    waitForMarkers(new DefaultEditor(),
      tuple("Remove this unnecessary cast to \"int\".", 9));

    var editor = new TextEditor(FILE_NAME);
    editor.activate();
    applyFixThrough(findQuickFixableMarkerInProblemsView().openQuickFix());

    new WaitUntil(new TextEditorContainsText(editor, EXPECTED_TEXT));
  }

  @Test
  public void shouldApplyQuickFixThroughOnTheFlyViewContextMenu() {
    openQuickFixableFile();
    waitForMarkers(new DefaultEditor(),
      tuple("Remove this unnecessary cast to \"int\".", 9));

    var editor = new TextEditor(FILE_NAME);
    editor.activate();
    new OnTheFlyView().selectItem(0);
    new ContextMenuItem("Quick Fix").select();
    applyFixThrough(new QuickFixWizard());

    new WaitUntil(new TextEditorContainsText(editor, EXPECTED_TEXT));
  }

  @Test
  public void shouldApplyQuickFixOnClosedFileThroughMarkerContextMenu() {
    openQuickFixableFile();
    waitForMarkers(new DefaultEditor(),
      tuple("Remove this unnecessary cast to \"int\".", 9));

    new TextEditor(FILE_NAME).close();

    applyFixThrough(findQuickFixableMarkerInProblemsView().openQuickFix());

    new WaitUntil(new TextEditorContainsText(new TextEditor(FILE_NAME), EXPECTED_TEXT));
  }

  @Test
  public void shouldApplyQuickFixInDirtyFile() {
    openQuickFixableFile();
    waitForMarkers(new DefaultEditor(),
      tuple("Remove this unnecessary cast to \"int\".", 9));

    var editor = new TextEditor(FILE_NAME);
    editor.insertText(0, PLACEHOLDER_LICENSE_HEADER);

    applyFixThrough(findQuickFixableMarkerInProblemsView().openQuickFix());

    new WaitUntil(new TextEditorContainsText(editor, EXPECTED_TEXT));
  }

  @Test
  public void shouldApplyQuickFixAfterFileModifiedOnFileSystem() throws IOException {
    var file = openQuickFixableFile();
    waitForMarkers(new DefaultEditor(),
      tuple("Remove this unnecessary cast to \"int\".", 9));

    var editor = new TextEditor(FILE_NAME);
    var ioFile = ResourcesPlugin.getWorkspace().getRoot().getProject(QUICK_FIXES_PROJECT_NAME).getFile("src/hello/" + FILE_NAME).getLocation().toFile();
    Files.write(ioFile.toPath(), (PLACEHOLDER_LICENSE_HEADER + editor.getText()).getBytes());
    file.refresh();
    editor.activate();
    new WaitUntil(new TextEditorContainsText(editor, PLACEHOLDER_LICENSE_HEADER));

    applyFixThrough(findQuickFixableMarkerInProblemsView().openQuickFix());

    new WaitUntil(new TextEditorContainsText(editor, EXPECTED_TEXT));
  }

  @Test
  public void shouldNotProposeQuickFixWhenTargetRangeIsInvalid() {
    openQuickFixableFile();
    waitForMarkers(new DefaultEditor(),
      tuple("Remove this unnecessary cast to \"int\".", 9));

    new TextEditor(FILE_NAME).insertText(8, 14, "random change");

    var quickFixWizard = findQuickFixableMarkerInProblemsView().openQuickFix();
    var quickFixProposed = new DefaultTable(quickFixWizard, 0).containsItem(QUICK_FIX_MESSAGE);
    quickFixWizard.cancel();

    assertThat(quickFixProposed).isFalse();
  }

  private Resource openQuickFixableFile() {
    var file = rootProject.getResource("src", "hello", FILE_NAME);
    openFileAndWaitForAnalysisCompletion(file);
    return file;
  }

  private static void applyFixThrough(QuickFixWizard wizard) {
    new DefaultTable(wizard, 0).select(QUICK_FIX_MESSAGE);
    new PushButton(wizard, "Select All").click();
    wizard.finish();
  }

  private static void changeSonarLintMarkerSeverity(MarkerSeverity severity) {
    var preferenceDialog = openPreferenceDialog();
    var preferences = new SonarLintPreferences(preferenceDialog);
    preferenceDialog.select(preferences);
    preferences.setMarkersSeverity(severity);
    preferenceDialog.ok();
  }

  private static AbstractMarker findQuickFixableMarkerInProblemsView() {
    var matchers = new AbstractMarkerMatcher[] {new MarkerTypeMatcher("SonarLint On-The-Fly Issue"), new MarkerDescriptionMatcher(ISSUE_MESSAGE)};
    new WaitUntil(new ProblemExists(ProblemType.WARNING, matchers));
    return new ProblemsView().getProblems(ProblemType.WARNING, matchers).get(0);
  }

}
