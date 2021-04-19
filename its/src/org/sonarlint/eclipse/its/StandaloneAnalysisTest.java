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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.eclipse.core.resources.DefaultProject;
import org.eclipse.reddeer.eclipse.core.resources.Project;
import org.eclipse.reddeer.eclipse.core.resources.Resource;
import org.eclipse.reddeer.eclipse.jdt.ui.packageview.PackageExplorerPart;
import org.eclipse.reddeer.eclipse.ui.dialogs.PropertyDialog;
import org.eclipse.reddeer.eclipse.ui.perspectives.JavaPerspective;
import org.eclipse.reddeer.eclipse.ui.wizards.datatransfer.ExternalProjectImportWizardDialog;
import org.eclipse.reddeer.eclipse.ui.wizards.datatransfer.WizardProjectsImportPage;
import org.eclipse.reddeer.eclipse.ui.wizards.datatransfer.WizardProjectsImportPage.ImportProject;
import org.eclipse.reddeer.jface.condition.WindowIsAvailable;
import org.eclipse.reddeer.swt.api.Button;
import org.eclipse.reddeer.swt.api.MenuItem;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.menu.ContextMenu;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.impl.editor.DefaultEditor;
import org.eclipse.reddeer.workbench.impl.editor.Marker;
import org.eclipse.reddeer.workbench.impl.editor.TextEditor;
import org.eclipse.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;
import org.hamcrest.core.StringContains;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.sonarlint.eclipse.its.reddeer.conditions.OnTheFlyViewIsEmpty;
import org.sonarlint.eclipse.its.reddeer.perspectives.PhpPerspective;
import org.sonarlint.eclipse.its.reddeer.perspectives.PydevPerspective;
import org.sonarlint.eclipse.its.reddeer.preferences.SonarLintPreferences;
import org.sonarlint.eclipse.its.reddeer.preferences.SonarLintProperties;
import org.sonarlint.eclipse.its.reddeer.views.OnTheFlyView;
import org.sonarlint.eclipse.its.reddeer.views.PydevPackageExplorer;
import org.sonarlint.eclipse.its.reddeer.views.ReportView;
import org.sonarlint.eclipse.its.reddeer.views.SonarLintIssue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assume.assumeTrue;

public class StandaloneAnalysisTest extends AbstractSonarLintTest {

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void shouldAnalyseJava() {
    System.out.println("shouldAnalyseJava");
    Assume.assumeFalse(platformVersion().toString().startsWith("4.4"));

    new JavaPerspective().open();
    Project rootProject = importExistingProjectIntoWorkspace("java/java-simple", "java-simple");

    OnTheFlyView onTheFlyView = new OnTheFlyView();
    onTheFlyView.open();

    Resource helloJavaFile = rootProject.getResource("src", "hello", "Hello.java");
    openFileAndWaitForAnalysisCompletion(helloJavaFile);

    DefaultEditor defaultEditor = new DefaultEditor();
    assertThat(defaultEditor.getMarkers())
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsExactly(tuple("Replace this use of System.out or System.err by a logger.", 9));
    defaultEditor.close();

    // clear marker (probably a better way to do that)
    new ContextMenu(onTheFlyView.getItems().get(0)).getItem("Delete").select();
    new PushButton(new DefaultShell("Delete Selected Entries"), "Delete").click();
    new WaitUntil(new OnTheFlyViewIsEmpty(onTheFlyView));

    rootProject.select();
    PropertyDialog dialog = new PropertyDialog(rootProject.getName());
    dialog.open();

    int analysisJobCountBefore = scheduledAnalysisJobCount.get();

    SonarLintProperties sonarLintProperties = new SonarLintProperties(dialog);
    dialog.select(sonarLintProperties);
    sonarLintProperties.disableAutomaticAnalysis();
    dialog.ok();

    helloJavaFile.open();

    TextEditor textEditor = new TextEditor();
    assertThat(textEditor.getMarkers()).isEmpty();

    textEditor.insertText(8, 29, "2");
    textEditor.save();

    assertThat(textEditor.getMarkers()).isEmpty();

    assertThat(scheduledAnalysisJobCount.get()).isEqualTo(analysisJobCountBefore);

    // Trigger manual analysis of a single file
    doAndWaitForSonarLintAnalysisJob(() -> new ContextMenu(helloJavaFile.getTreeItem()).getItem("SonarLint", "Analyze").select());

    assertThat(textEditor.getMarkers())
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsExactly(tuple("Replace this use of System.out or System.err by a logger.", 9));

    // Trigger manual analysis of all files
    rootProject.getTreeItem().select();
    doAndWaitForSonarLintAnalysisJob(() -> {
      MenuItem menuItem = new ContextMenu(rootProject.getTreeItem()).getItem("SonarLint", "Analyze");
      menuItem.select();
      new PushButton(new DefaultShell("Confirmation"), "OK").click();
    });

    ReportView reportView = new ReportView();
    List<TreeItem> items = reportView.getItems();

    assertThat(items)
      .extracting(item -> item.getCell(0), item -> item.getCell(2))
      .containsExactlyInAnyOrder(
        tuple("Hello.java", "Replace this use of System.out or System.err by a logger."),
        tuple("Hello2.java", "Replace this use of System.out or System.err by a logger."));
  }

  @Test
  public void shouldAnalyseJavaJunit() {
    System.out.println("shouldAnalyseJavaJunit");
    assumeTrue(supportJunit());
    new JavaPerspective().open();
    Project rootProject = importExistingProjectIntoWorkspace("java/java-junit", "java-junit");

    WorkbenchPreferenceDialog preferenceDialog = new WorkbenchPreferenceDialog();
    preferenceDialog.open();
    SonarLintPreferences preferences = new SonarLintPreferences(preferenceDialog);
    preferenceDialog.select(preferences);
    preferences.setTestFileRegularExpressions("**/*TestUtil*");
    preferenceDialog.ok();

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "hello", "Hello.java"));

    DefaultEditor defaultEditor = new DefaultEditor();
    assertThat(defaultEditor.getMarkers())
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsOnly(
        tuple("Replace this use of System.out or System.err by a logger.", 12),
        tuple("Remove this unnecessary cast to \"int\".", 16)); // Test that sonar.java.libraries is set

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "hello", "HelloTestUtil.java"));

    defaultEditor = new DefaultEditor();
    assertThat(defaultEditor.getMarkers())
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsExactly(
        // File is flagged as test by regexp, only test rules are applied
        tuple("Remove this use of \"Thread.sleep()\".", 11));

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("tests", "hello", "HelloTest.java"));

    defaultEditor = new DefaultEditor();
    assertThat(defaultEditor.getMarkers())
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsOnly(
        tuple("Either add an explanation about why this test is skipped or remove the \"@Ignore\" annotation.", 10),
        tuple("Add at least one assertion to this test case.", 10));
  }

  @Test
  public void shouldAnalyseJava8() {
    System.out.println("shouldAnalyseJava8");

    assumeTrue(supportJava8());
    new JavaPerspective().open();
    Project rootProject = importExistingProjectIntoWorkspace("java/java8", "java8");

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "hello", "Hello.java"));

    DefaultEditor defaultEditor = new DefaultEditor();
    assertThat(defaultEditor.getMarkers())
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsOnly(
        tuple("Make this anonymous inner class a lambda", 13),
        tuple("Refactor the code so this stream pipeline is used.", 13)); // Test that sonar.java.source is set
  }

  // SONARIDE-349
  // SONARIDE-350
  // SONARIDE-353
  @Test
  public void shouldAnalyseJavaWithDependentProject() {
    System.out.println("shouldAnalyseJavaWithDependentProject");

    new JavaPerspective().open();
    importExistingProjectIntoWorkspace("java/java-dependent-projects/java-dependent-project");
    Project rootProject = importExistingProjectIntoWorkspace("java/java-dependent-projects/java-main-project", "java-main-project");

    File toBeDeleted = new File(ResourcesPlugin.getWorkspace().getRoot().getProject("java-main-project").getLocation().toFile(), "libs/toBeDeleted.jar");
    assertThat(toBeDeleted.delete()).as("Unable to delete JAR to test SONARIDE-350").isTrue();

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "use", "UseUtils.java"));

    DefaultEditor defaultEditor = new DefaultEditor();
    assertThat(defaultEditor.getMarkers())
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsOnly(tuple("Remove this unnecessary cast to \"int\".", 9)); // Test that sonar.java.libraries is set on dependent project
  }

  // Need PyDev
  @Test
  @Category(RequiresExtraDependency.class)
  public void shouldAnalysePython() {
    System.out.println("shouldAnalysePython");
    new PydevPerspective().open();
    importPythonProjectIntoWorkspace("python");

    OnTheFlyView onTheFlyView = new OnTheFlyView();
    onTheFlyView.open();
    // workaround a view refresh problem
    onTheFlyView.close();
    onTheFlyView.open();

    DefaultProject rootProject = new PydevPackageExplorer().getProject("python");
    rootProject.getTreeItem().select();
    doAndWaitForSonarLintAnalysisJob(() -> {
      open(rootProject.getResource("src", "root", "nested", "example.py"));

      new WaitUntil(new ShellIsAvailable("Default Eclipse preferences for PyDev"));
      new PushButton(new DefaultShell("Default Eclipse preferences for PyDev"), "OK").click();
    });

    assertThat(onTheFlyView.getIssues())
      .extracting(SonarLintIssue::getDescription, SonarLintIssue::getResource)
      .containsOnly(
        tuple("Merge this if statement with the enclosing one. [+1 location]", "example.py"),
        tuple("Replace print statement by built-in function.", "example.py"),
        tuple("Replace \"<>\" by \"!=\".", "example.py"));
  }

  private static void importPythonProjectIntoWorkspace(String relativePathFromProjectsFolder) {
    ExternalProjectImportWizardDialog dialog = new ExternalProjectImportWizardDialog();
    dialog.open();
    WizardProjectsImportPage importPage = new WizardProjectsImportPage(dialog);
    importPage.copyProjectsIntoWorkspace(true);
    importPage.setRootDirectory(new File("projects", relativePathFromProjectsFolder).getAbsolutePath());
    List<ImportProject> projects = importPage.getProjects();
    assertThat(projects).hasSize(1);
    Button button = new FinishButton(dialog);
    button.click();
    new PushButton(new DefaultShell("Python not configured"), "Don't ask again").click();

    new WaitWhile(new WindowIsAvailable(dialog), TimePeriod.LONG);
    try {
      new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
    } catch (NoClassDefFoundError e) {
      // do nothing, reddeer.workbench plugin is not available
    }
  }

  // Need PDT
  @Test
  @Category(RequiresExtraDependency.class)
  public void shouldAnalysePHP() {
    System.out.println("shouldAnalysePHP");

    new PhpPerspective().open();
    Project rootProject = importExistingProjectIntoWorkspace("php", "php");
    new WaitWhile(new JobIsRunning(StringContains.containsString("DLTK Indexing")), TimePeriod.LONG);

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("foo.php"));

    OnTheFlyView onTheFlyView = new OnTheFlyView();
    onTheFlyView.open();
    assertThat(onTheFlyView.getIssues())
      .extracting(SonarLintIssue::getDescription, SonarLintIssue::getResource)
      .containsOnly(tuple("This branch duplicates the one on line 5. [+1 location]", "foo.php"));

    // SLE-342
    openFileAndWaitForAnalysisCompletion(rootProject.getResource("foo.inc"));

    assertThat(onTheFlyView.getIssues())
      .extracting(SonarLintIssue::getDescription, SonarLintIssue::getResource)
      .containsOnly(tuple("This branch duplicates the one on line 5. [+1 location]", "foo.inc"));
  }

  @Test
  public void shouldAnalyseLinkedFile() throws IOException {
    System.out.println("shouldAnalyseLinkedFile");
    new JavaPerspective().open();
    Project rootProject = importExistingProjectIntoWorkspace("java/java-linked", "java-linked");

    File dotProject = new File(ResourcesPlugin.getWorkspace().getRoot().getProject("java-linked").getLocation().toFile(), ".project");
    String content = FileUtils.readFileToString(dotProject, StandardCharsets.UTF_8);
    FileUtils.write(dotProject, content.replace("${PLACEHOLDER}", new File("projects/java/java-linked-target/hello/HelloLinked.java").getAbsolutePath()), StandardCharsets.UTF_8);

    rootProject.refresh();

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "hello", "HelloLinked.java"));

    DefaultEditor defaultEditor = new DefaultEditor("HelloLinked.java");
    assertThat(defaultEditor.getMarkers())
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsOnly(tuple("Replace this use of System.out or System.err by a logger.", 13));
  }

  // Need RSE
  @Test
  @Category(RequiresExtraDependency.class)
  public void shouldAnalyseVirtualProject() throws Exception {
    System.out.println("shouldAnalyseVirtualProject");

    File remoteProjectDir = temp.newFolder();
    FileUtils.copyDirectory(new File("projects/java/java-simple"), remoteProjectDir);

    new JavaPerspective().open();
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    final IProject rseProject = workspace.getRoot().getProject("Local_java-simple");

    workspace.run(new IWorkspaceRunnable() {

      @Override
      public void run(final IProgressMonitor monitor) throws CoreException {
        final IProjectDescription projectDescription = workspace.newProjectDescription(rseProject.getName());
        URI uri = remoteProjectDir.toURI();
        try {
          projectDescription.setLocationURI(new URI("rse", "LOCALHOST", uri.getPath(), null));
        } catch (URISyntaxException e) {
          throw new IllegalStateException(e);
        }
        rseProject.create(projectDescription, monitor);
        rseProject.open(IResource.NONE, monitor);
      }
    }, workspace.getRoot(), IWorkspace.AVOID_UPDATE, new NullProgressMonitor());

    PackageExplorerPart packageExplorer = new PackageExplorerPart();
    packageExplorer.open();
    DefaultProject rootProject = packageExplorer.getProject("Local_java-simple");
    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "hello", "Hello.java"));

    DefaultEditor defaultEditor = new DefaultEditor();
    assertThat(defaultEditor.getMarkers())
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsOnly(tuple("Replace this use of System.out or System.err by a logger.", 9));
  }

}
