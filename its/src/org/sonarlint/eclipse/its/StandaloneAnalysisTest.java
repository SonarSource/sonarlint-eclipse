/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.apache.commons.io.FileUtils;
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
import org.eclipse.reddeer.eclipse.jdt.ui.packageview.PackageExplorerPart;
import org.eclipse.reddeer.eclipse.ui.dialogs.PropertyDialog;
import org.eclipse.reddeer.eclipse.ui.perspectives.JavaPerspective;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.link.DefaultLink;
import org.eclipse.reddeer.swt.impl.menu.ContextMenu;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.impl.editor.DefaultEditor;
import org.eclipse.reddeer.workbench.impl.editor.Marker;
import org.eclipse.reddeer.workbench.impl.editor.TextEditor;
import org.eclipse.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;
import org.hamcrest.core.StringContains;
import org.junit.After;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonarlint.eclipse.its.reddeer.conditions.OnTheFlyViewIsEmpty;
import org.sonarlint.eclipse.its.reddeer.dialogs.EnhancedWithConnectedModeInformationDialog;
import org.sonarlint.eclipse.its.reddeer.perspectives.PhpPerspective;
import org.sonarlint.eclipse.its.reddeer.perspectives.PydevPerspective;
import org.sonarlint.eclipse.its.reddeer.preferences.GeneralWorkspaceBuildPreferences;
import org.sonarlint.eclipse.its.reddeer.preferences.SonarLintPreferences;
import org.sonarlint.eclipse.its.reddeer.preferences.SonarLintProperties;
import org.sonarlint.eclipse.its.reddeer.views.OnTheFlyView;
import org.sonarlint.eclipse.its.reddeer.views.PydevPackageExplorer;
import org.sonarlint.eclipse.its.reddeer.views.ReportView;
import org.sonarlint.eclipse.its.reddeer.views.SonarLintIssueMarker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class StandaloneAnalysisTest extends AbstractSonarLintTest {
  @Before
  public void removeEnhancedConnectedModeInformationPerTest() {
    System.setProperty("sonarlint.internal.ignoreEnhancedFeature", "true");
    System.setProperty("sonarlint.internal.ignoreMissingFeature", "true");
    System.setProperty("sonarlint.internal.ignoreNoAutomaticBuildWarning", "true");
  }
  
  @After
  public void enableAutomaticWorkspaceBuild() {
    if ("oldest".equals(System.getProperty("target.platform"))) {
      var preferences = GeneralWorkspaceBuildPreferences.open();
      preferences.enableAutomaticBuild();
      preferences.ok();
    }
  }
  
  @Test
  public void analyze_automatic_workspace_build_disabled() {
    // INFO: We only want to run it on one axis and the "oldest" ITs take the shortest!
    Assume.assumeTrue("oldest".equals(System.getProperty("target.platform")));
    
    System.clearProperty("sonarlint.internal.ignoreNoAutomaticBuildWarning");
    
    // 1) Configure preferences
    var preferences = GeneralWorkspaceBuildPreferences.open();
    preferences.disableAutomaticBuild();
    preferences.ok();
    
    // 2) Import project
    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("java/java-simple", "java-simple");

    // 3) Open file and click pop-up but don't enable automatic build
    var helloJavaFile = rootProject.getResource("src", "hello", "Hello.java");
    openFileAndWaitForAnalysisCompletion(helloJavaFile);
    new DefaultEditor().close();
    
    var popUp = new DefaultShell("Automatic build of workspace disabled");
    new DefaultLink(popUp, "Enable automatic build of workspace").click();
    preferences = GeneralWorkspaceBuildPreferences.open();
    preferences.ok();
    
    // 4) Open file and click pop-up but don't show again
    helloJavaFile = rootProject.getResource("src", "hello", "Hello.java");
    openFileAndWaitForAnalysisCompletion(helloJavaFile);
    new DefaultEditor().close();
    
    popUp = new DefaultShell("Automatic build of workspace disabled");
    new DefaultLink(popUp, "Don't show again").click();
    
    // 5) Reset preferences
    preferences = GeneralWorkspaceBuildPreferences.open();
    preferences.enableAutomaticBuild();
    preferences.ok();
  }
  
  @Test
  public void analyzeProjectWithMissingLanguageAnalyzers() {
    // INFO: This test case should display everything!
    System.clearProperty("sonarlint.internal.ignoreEnhancedFeature");
    System.clearProperty("sonarlint.internal.ignoreMissingFeature");
    
    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("connected", "connected");

    var abapFile = rootProject.getResource("Test.abap");
    openFileAndWaitForAnalysisCompletion(abapFile);
    
    var notAnalyzed = new DefaultShell("SonarLint - Language could not be analyzed");
    new DefaultLink(notAnalyzed, "Learn more").click();
    new DefaultLink(notAnalyzed, "Try SonarCloud for free").click();
    notAnalyzed.close();
    
    new ContextMenu(rootProject.getTreeItem()).getItem("SonarLint", "Analyze").select();
    var dialog = new EnhancedWithConnectedModeInformationDialog("Are you working with a CI/CD pipeline?");
    doAndWaitForSonarLintAnalysisJob(dialog::learnMore);
    
    notAnalyzed = new DefaultShell("SonarLint - Languages could not be analyzed");
    new DefaultLink(notAnalyzed, "Don't show again").click();
    
    new ContextMenu(rootProject.getTreeItem()).getItem("SonarLint", "Analyze").select();
    var dialog2 = new EnhancedWithConnectedModeInformationDialog("Are you working with a CI/CD pipeline?");
    doAndWaitForSonarLintAnalysisJob(dialog2::trySonarCloudForFree);
    
    new ContextMenu(rootProject.getTreeItem()).getItem("SonarLint", "Analyze").select();
    var dialog3 = new EnhancedWithConnectedModeInformationDialog("Are you working with a CI/CD pipeline?");
    doAndWaitForSonarLintAnalysisJob(dialog3::dontAskAgain);
    
    doAndWaitForSonarLintAnalysisJob(
      () -> new ContextMenu(rootProject.getTreeItem()).getItem("SonarLint", "Analyze").select());
  }

  @Test
  public void shouldAnalyseJava() {
    Assume.assumeFalse(platformVersion().toString().startsWith("4.4"));

    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("java/java-simple", "java-simple");

    var onTheFlyView = new OnTheFlyView();
    onTheFlyView.open();

    var helloJavaFile = rootProject.getResource("src", "hello", "Hello.java");
    openFileAndWaitForAnalysisCompletion(helloJavaFile);

    var defaultEditor = new DefaultEditor();
    assertThat(defaultEditor.getMarkers())
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsExactly(tuple("Replace this use of System.out by a logger.", 9));
    defaultEditor.close();

    // clear marker (probably a better way to do that)
    onTheFlyView.getIssues().get(0).delete();
    new PushButton(new DefaultShell("Delete Selected Entries"), "Delete").click();
    new WaitUntil(new OnTheFlyViewIsEmpty(onTheFlyView));

    rootProject.select();
    var dialog = new PropertyDialog(rootProject.getName());
    dialog.open();

    var analysisJobCountBefore = scheduledAnalysisJobCount.get();

    var sonarLintProperties = new SonarLintProperties(dialog);
    dialog.select(sonarLintProperties);
    sonarLintProperties.disableAutomaticAnalysis();
    dialog.ok();

    helloJavaFile.open();

    var textEditor = new TextEditor();
    assertThat(textEditor.getMarkers()).isEmpty();

    textEditor.insertText(8, 29, "2");
    textEditor.save();

    assertThat(textEditor.getMarkers()).isEmpty();

    assertThat(scheduledAnalysisJobCount.get()).isEqualTo(analysisJobCountBefore);

    // Trigger manual analysis of a single file
    doAndWaitForSonarLintAnalysisJob(() -> new ContextMenu(helloJavaFile.getTreeItem()).getItem("SonarLint", "Analyze").select());

    assertThat(textEditor.getMarkers())
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsExactly(tuple("Replace this use of System.out by a logger.", 9));

    // Trigger manual analysis of all files
    rootProject.select();
    new ContextMenu(rootProject.getTreeItem()).getItem("SonarLint", "Analyze").select();
    doAndWaitForSonarLintAnalysisJob(() -> new OkButton(new DefaultShell("Confirmation")).click());

    var reportView = new ReportView();
    var items = reportView.getItems();

    assertThat(items)
      .extracting(item -> item.getCell(0), item -> item.getCell(2))
      .containsExactlyInAnyOrder(
        tuple("Hello.java", "Replace this use of System.out by a logger."),
        tuple("Hello2.java", "Replace this use of System.out by a logger."),
        tuple("Hello3.java", "Replace this use of System.out by a logger."),
        tuple("Hello4.java", "Replace this use of System.out by a logger."),
        tuple("Hello5.java", "Replace this use of System.out by a logger."),
        tuple("Hello6.java", "Replace this use of System.out by a logger."),
        tuple("Hello7.java", "Replace this use of System.out by a logger."),
        tuple("Hello8.java", "Replace this use of System.out by a logger."),
        tuple("Hello9.java", "Replace this use of System.out by a logger."),
        tuple("Hello10.java", "Replace this use of System.out by a logger."),
        tuple("Hello11.java", "Replace this use of System.out by a logger."));
  }

  @Test
  public void shouldAnalyseJavaJunit() {
    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("java/java-junit", "java-junit");

    var preferenceDialog = new WorkbenchPreferenceDialog();
    preferenceDialog.open();
    var preferences = new SonarLintPreferences(preferenceDialog);
    preferenceDialog.select(preferences);
    preferences.setTestFileRegularExpressions("**/*TestUtil*");
    preferenceDialog.ok();

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "hello", "Hello.java"));

    var defaultEditor = new DefaultEditor();
    assertThat(defaultEditor.getMarkers())
      .filteredOn(m -> ON_THE_FLY_ANNOTATION_TYPE.equals(m.getType()))
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsOnly(
        tuple("Replace this use of System.out by a logger.", 12),
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
    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("java/java8", "java8");

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "hello", "Hello.java"));

    var defaultEditor = new DefaultEditor();
    assertThat(defaultEditor.getMarkers())
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsOnly(
        tuple("Make this anonymous inner class a lambda", 13),
        tuple("Refactor the code so this stream pipeline is used.", 13)); // Test that sonar.java.source is set
  }

  @Test
  public void shouldAnalyseJsInYamlFile() {
    // Don't run this test on macOS devices as Node.js might not be found!
    ignoreMacOS();
    
    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("js/js-simple", "js-simple");

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("lambda.yaml"));

    var defaultEditor = new DefaultEditor();
    assertThat(defaultEditor.getMarkers())
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsOnly(
        tuple("Remove the declaration of the unused 'x' variable.", 9));
  }

  @Test
  public void shouldAnalyseCSS() {
    // Don't run this test on macOS devices as Node.js might not be found!
    ignoreMacOS();
    
    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("css/css-simple", "css-simple");

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("file.css"));

    var defaultEditor = new DefaultEditor();
    assertThat(defaultEditor.getMarkers())
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsOnly(
        tuple("Unexpected double-slash CSS comment", 1));
  }

  @Test
  public void shouldAnalyseTypeScript() {
    // Don't run this test on macOS devices as Node.js might not be found!
    ignoreMacOS();
    
    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("ts/ts-simple", "ts-simple");

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("file.ts"));

    var defaultEditor = new DefaultEditor();
    assertThat(defaultEditor.getMarkers())
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsOnly(
        tuple("This loop's stop condition tests \"i\" but the incrementer updates \"j\".", 2));
  }

  // SONARIDE-349
  // SONARIDE-350
  // SONARIDE-353
  @Test
  public void shouldAnalyseJavaWithDependentProject() {
    new JavaPerspective().open();
    importExistingProjectIntoWorkspace("java/java-dependent-projects/java-dependent-project");
    var rootProject = importExistingProjectIntoWorkspace("java/java-dependent-projects/java-main-project", "java-main-project");

    var toBeDeleted = new File(ResourcesPlugin.getWorkspace().getRoot().getProject("java-main-project").getLocation().toFile(), "libs/toBeDeleted.jar");
    assertThat(toBeDeleted.delete()).as("Unable to delete JAR to test SONARIDE-350").isTrue();

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "use", "UseUtils.java"));

    var defaultEditor = new DefaultEditor();
    assertThat(defaultEditor.getMarkers())
      .filteredOn(m -> ON_THE_FLY_ANNOTATION_TYPE.equals(m.getType()))
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsOnly(tuple("Remove this unnecessary cast to \"int\".", 9)); // Test that sonar.java.libraries is set on dependent project
  }

  // Need PyDev
  @Test
  @Ignore("12/2023: Removal of iBuilds axis, PyDev has to be updated in latest.target to work")
  @Category(RequiresExtraDependency.class)
  public void shouldAnalysePython() {
    // The PydevPerspective is not working correctly in older PyDev versions, therefore only run in latest
    Assume.assumeTrue("latest".equals(System.getProperty("target.platform", "latest")));
    
    new PydevPerspective().open();
    importExistingProjectIntoWorkspace("python");

    var rootProject = new PydevPackageExplorer().getProject("python");
    rootProject.getTreeItem().select();
    doAndWaitForSonarLintAnalysisJob(() -> {
      open(rootProject.getResource("src", "root", "nested", "example.py"));

      new WaitUntil(new ShellIsAvailable("Default Eclipse preferences for PyDev"));
      new OkButton(new DefaultShell("Default Eclipse preferences for PyDev")).click();
    });

    var defaultEditor = new DefaultEditor();
    assertThat(defaultEditor.getMarkers())
      .filteredOn(m -> ON_THE_FLY_ANNOTATION_TYPE.equals(m.getType()))
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsOnly(tuple("Merge this if statement with the enclosing one.", 9),
        tuple("Replace print statement by built-in function.", 10),
        tuple("Replace \"<>\" by \"!=\".", 9));
  }

  // Need PDT
  @Test
  @Category(RequiresExtraDependency.class)
  public void shouldAnalysePHP() {
    new PhpPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("php", "php");
    new WaitWhile(new JobIsRunning(StringContains.containsString("DLTK Indexing")), TimePeriod.LONG);

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("foo.php"));

    var onTheFlyView = new OnTheFlyView();
    onTheFlyView.open();
    assertThat(onTheFlyView.getIssues())
      .extracting(SonarLintIssueMarker::getDescription, SonarLintIssueMarker::getResource)
      .contains(tuple("This branch duplicates the one on line 5. [+1 location]", "foo.php"));

    // SLE-342
    openFileAndWaitForAnalysisCompletion(rootProject.getResource("foo.inc"));

    assertThat(onTheFlyView.getIssues())
      .extracting(SonarLintIssueMarker::getDescription, SonarLintIssueMarker::getResource)
      .contains(tuple("This branch duplicates the one on line 5. [+1 location]", "foo.inc"));
  }

  @Test
  public void shouldAnalyseLinkedFile() throws IOException {
    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("java/java-linked", "java-linked");

    var dotProject = new File(ResourcesPlugin.getWorkspace().getRoot().getProject("java-linked").getLocation().toFile(), ".project");
    var content = FileUtils.readFileToString(dotProject, StandardCharsets.UTF_8);
    FileUtils.write(dotProject, content.replace("${PLACEHOLDER}", new File("projects/java/java-linked-target/hello/HelloLinked.java").getAbsolutePath()), StandardCharsets.UTF_8);

    rootProject.refresh();

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "hello", "HelloLinked.java"));

    var defaultEditor = new DefaultEditor("HelloLinked.java");
    assertThat(defaultEditor.getMarkers())
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsOnly(tuple("Replace this use of System.out by a logger.", 13));
  }

  // Need RSE
  @Test
  @Category(RequiresExtraDependency.class)
  public void shouldAnalyseVirtualProject() throws Exception {
    var remoteProjectDir = tempFolder.newFolder();
    FileUtils.copyDirectory(new File("projects/java/java-simple"), remoteProjectDir);

    new JavaPerspective().open();
    var workspace = ResourcesPlugin.getWorkspace();
    final var rseProject = workspace.getRoot().getProject("Local_java-simple");

    workspace.run(new IWorkspaceRunnable() {

      @Override
      public void run(final IProgressMonitor monitor) throws CoreException {
        final var projectDescription = workspace.newProjectDescription(rseProject.getName());
        var uri = remoteProjectDir.toURI();
        try {
          projectDescription.setLocationURI(new URI("rse", "LOCALHOST", uri.getPath(), null));
        } catch (URISyntaxException e) {
          throw new IllegalStateException(e);
        }
        rseProject.create(projectDescription, monitor);
        rseProject.open(IResource.NONE, monitor);
      }
    }, workspace.getRoot(), IWorkspace.AVOID_UPDATE, new NullProgressMonitor());

    var packageExplorer = new PackageExplorerPart();
    packageExplorer.open();
    var rootProject = packageExplorer.getProject("Local_java-simple");
    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "hello", "Hello.java"));

    var defaultEditor = new DefaultEditor();
    assertThat(defaultEditor.getMarkers())
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsOnly(tuple("Replace this use of System.out by a logger.", 9));
  }

}
