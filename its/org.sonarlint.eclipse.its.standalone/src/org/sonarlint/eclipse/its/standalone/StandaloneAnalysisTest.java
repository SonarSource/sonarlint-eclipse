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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.awaitility.Awaitility;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.eclipse.condition.ProjectExists;
import org.eclipse.reddeer.eclipse.jdt.ui.packageview.PackageExplorerPart;
import org.eclipse.reddeer.eclipse.ui.dialogs.PropertyDialog;
import org.eclipse.reddeer.eclipse.ui.markers.matcher.MarkerDescriptionMatcher;
import org.eclipse.reddeer.eclipse.ui.perspectives.JavaPerspective;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.link.DefaultLink;
import org.eclipse.reddeer.swt.impl.menu.ContextMenu;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.workbench.impl.editor.DefaultEditor;
import org.eclipse.reddeer.workbench.impl.editor.TextEditor;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sonarlint.eclipse.its.shared.AbstractSonarLintTest;
import org.sonarlint.eclipse.its.shared.reddeer.conditions.ConfirmManualAnalysisDialogOpened;
import org.sonarlint.eclipse.its.shared.reddeer.conditions.EnhancedWithConnectedModeInformationDialogOpened;
import org.sonarlint.eclipse.its.shared.reddeer.conditions.OnTheFlyViewIsEmpty;
import org.sonarlint.eclipse.its.shared.reddeer.dialogs.ConfirmManualAnalysisDialog;
import org.sonarlint.eclipse.its.shared.reddeer.dialogs.EnhancedWithConnectedModeInformationDialog;
import org.sonarlint.eclipse.its.shared.reddeer.preferences.GeneralWorkspaceBuildPreferences;
import org.sonarlint.eclipse.its.shared.reddeer.preferences.SonarLintPreferences;
import org.sonarlint.eclipse.its.shared.reddeer.preferences.SonarLintProperties;
import org.sonarlint.eclipse.its.shared.reddeer.views.OnTheFlyView;
import org.sonarlint.eclipse.its.shared.reddeer.views.ReportView;
import org.sonarlint.eclipse.its.shared.reddeer.views.SonarLintConsole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

public class StandaloneAnalysisTest extends AbstractSonarLintTest {
  @Before
  public void removeEnhancedConnectedModeInformationPerTest() {
    System.setProperty("sonarlint.internal.ignoreEnhancedFeature", "true");
    System.setProperty("sonarlint.internal.ignoreMissingFeature", "true");
    System.setProperty("sonarlint.internal.ignoreNoAutomaticBuildWarning", "true");
  }

  @After
  public void enableAutomaticWorkspaceBuild() {
    if ("oldest-java-11_e417".equals(System.getProperty("target.platform"))) {
      var preferences = GeneralWorkspaceBuildPreferences.open();
      preferences.enableAutomaticBuild();
      preferences.ok();
    }
  }

  @AfterClass
  public static void disableIdeSpecificTraces() {
    new SonarLintConsole().enableIdeSpecificLogs(false);
  }

  @Test
  public void analyze_automatic_workspace_build_disabled() {
    // INFO: We only want to run it on one axis and the "oldest" ITs take the shortest!
    Assume.assumeTrue("oldest-java-11_e417".equals(System.getProperty("target.platform")));

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

    shellByName("Automatic build of workspace disabled")
      .ifPresent(shell -> new DefaultLink(shell, "Enable automatic build of workspace").click());
    preferences = GeneralWorkspaceBuildPreferences.open();
    preferences.ok();

    // 4) Open file and click pop-up but don't show again
    helloJavaFile = rootProject.getResource("src", "hello", "Hello.java");
    openFileAndWaitForAnalysisCompletion(helloJavaFile);
    new DefaultEditor().close();

    shellByName("Automatic build of workspace disabled")
      .ifPresent(shell -> new DefaultLink(shell, "Don't show again").click());

    // 5) Reset preferences
    preferences = GeneralWorkspaceBuildPreferences.open();
    preferences.enableAutomaticBuild();
    preferences.ok();
  }

  /** See SLE-854: JDT tries to find files with 'Java-like' extensions even if they're not Java */
  @Test
  public void test_jdt_java_like_extension_COBOL() {
    // INFO: Here we check for the IDE-specific logs, so we have to enable them!
    try {
      new SonarLintConsole().enableIdeSpecificLogs(true);

      new JavaPerspective().open();
      var rootProject = importExistingProjectIntoWorkspace("connected", "connected");

      var cobolFile = rootProject.getResource("Test.cbl");
      openFileAndWaitForAnalysisCompletion(cobolFile);

      // To see that the analysis ran and the file wasn't skipped by JDT, we await the "Analysis output" of the
      // SonarText analyzer starting.
      // To see that enabling the IDE-specific logs in the SonarLint Console worked, we wait for the adaptation
      // trace message of the SonarLintPostBuildListener coming in.
      // To see that the file wasn't excluded by JDT, we also check that that message is not in the logs: It cannot
      // happen if the SonarText analyzer starting message is coming and vice versa.
      Awaitility.await()
        .untilAsserted(() -> assertThat(new SonarLintConsole().getConsoleView().getConsoleText())
          .contains("Execute Sensor: TextAndSecretsSensor")
          .contains(
            "[SonarLintPostBuildListener#visitDelta] Try get project of resource 'L/connected/Test.cbl' -> 'L/connected/Test.cbl' could not be adapted to 'org.sonarlint.eclipse.core.resource.ISonarLintProject'")
          .doesNotContain("File 'Test.cbl' excluded by 'JavaProjectConfiguratorExtension'"));
    } finally {
      new SonarLintConsole().enableIdeSpecificLogs(false);
    }
  }

  @Test
  public void analyzeProjectWithMissingLanguageAnalyzers() {
    // INFO: This test case should display everything!
    System.clearProperty("sonarlint.internal.ignoreEnhancedFeature");
    System.clearProperty("sonarlint.internal.ignoreMissingFeature");

    // Title of the enhanced with Connected Mode dialog
    var dialogTitle = "Are you working with a CI/CD pipeline?";

    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("connected", "connected");

    var abapFile = rootProject.getResource("Test.abap");
    openFileAndWaitForAnalysisCompletion(abapFile);

    var notAnalyzedOpt = shellByName("SonarQube for Eclipse - Language could not be analyzed");
    notAnalyzedOpt.ifPresent(shell -> new DefaultLink(shell, "Learn more").click());
    notAnalyzedOpt.ifPresent(shell -> new DefaultLink(shell, "Try SonarQube Cloud for free").click());
    notAnalyzedOpt.ifPresent(DefaultShell::close);

    new ContextMenu(rootProject.getTreeItem()).getItem("SonarQube", "Analyze").select();
    new WaitUntil(new EnhancedWithConnectedModeInformationDialogOpened(dialogTitle));
    var dialog = new EnhancedWithConnectedModeInformationDialog(dialogTitle);
    doAndWaitForSonarLintAnalysisJob(dialog::learnMore);

    // THe project contains multiple languages, therefore the shell name slightly differs
    notAnalyzedOpt = shellByName("SonarQube for Eclipse - Languages could not be analyzed");
    notAnalyzedOpt.ifPresent(shell -> new DefaultLink(shell, "Don't show again").click());

    new ContextMenu(rootProject.getTreeItem()).getItem("SonarQube", "Analyze").select();
    new WaitUntil(new EnhancedWithConnectedModeInformationDialogOpened(dialogTitle));
    var dialog2 = new EnhancedWithConnectedModeInformationDialog(dialogTitle);
    doAndWaitForSonarLintAnalysisJob(dialog2::trySonarCloudForFree);

    new ContextMenu(rootProject.getTreeItem()).getItem("SonarQube", "Analyze").select();
    new WaitUntil(new EnhancedWithConnectedModeInformationDialogOpened(dialogTitle));
    var dialog3 = new EnhancedWithConnectedModeInformationDialog(dialogTitle);
    doAndWaitForSonarLintAnalysisJob(dialog3::dontAskAgain);

    doAndWaitForSonarLintAnalysisJob(
      () -> new ContextMenu(rootProject.getTreeItem()).getItem("SonarQube", "Analyze").select());
  }

  @Test
  public void shouldAnalyseJava() {
    var console = new SonarLintConsole();
    console.enableIdeSpecificLogs(true);
    console.clear();

    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("java/java-simple", "java-simple");

    var onTheFlyView = new OnTheFlyView();
    onTheFlyView.open();

    await().untilAsserted(
      () -> assertThat(new SonarLintConsole().getConsoleView().getConsoleText())
        .contains("[JdtUtils#getExcludedPaths] The following paths have been excluded from indexing for the project"));

    var helloJavaFile = rootProject.getResource("src", "hello", "Hello.java");
    openFileAndWaitForAnalysisCompletion(helloJavaFile);

    var defaultEditor = new DefaultEditor();
    waitForMarkers(defaultEditor,
      tuple("Replace this use of System.out by a logger.", 9));

    defaultEditor.close();

    // clear marker (probably a better way to do that)
    onTheFlyView.getIssues().get(0).delete();

    new PushButton(shellByName("Delete Selected Entries").get(), "Delete").click();
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
    waitForNoMarkers(textEditor);

    textEditor.insertText(8, 29, "2");
    textEditor.save();

    waitForNoMarkers(textEditor);

    assertThat(scheduledAnalysisJobCount.get()).isEqualTo(analysisJobCountBefore);

    var reportView = new ReportView();

    // Trigger manual analysis of a single file (should only add markers to the report view)
    doAndWaitForSonarLintAnalysisJob(() -> new ContextMenu(helloJavaFile.getTreeItem()).getItem("SonarQube", "Analyze").select());
    waitForNoMarkers(textEditor);

    // TODO: Implement for ReportView
    Awaitility.await()
      .atMost(20, TimeUnit.SECONDS)
      .untilAsserted(() -> {
        assertThat(reportView.getItems())
          .extracting(item -> item.getCell(0), item -> item.getCell(2))
          .containsExactlyInAnyOrder(
            tuple("Hello.java", "Replace this use of System.out by a logger."));
      });

    // Trigger manual analysis of all files
    rootProject.select();
    new ContextMenu(rootProject.getTreeItem()).getItem("SonarQube", "Analyze").select();
    new WaitUntil(new ConfirmManualAnalysisDialogOpened());
    doAndWaitForSonarLintAnalysisJob(() -> new ConfirmManualAnalysisDialog().ok());
    waitForNoMarkers(textEditor);

    Awaitility.await()
      .atMost(20, TimeUnit.SECONDS)
      .untilAsserted(() -> {
        assertThat(reportView.getItems())
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
      });
  }

  @Test
  public void shouldAnalyseJavaNoJdtExclusions() {
    var console = new SonarLintConsole();
    console.enableIdeSpecificLogs(true);
    console.clear();

    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("java/NoIndexSupport", "NoIndexSupport");

    var onTheFlyView = new OnTheFlyView();
    onTheFlyView.open();

    var helloJavaFile = rootProject.getResource("src", "hello", "Hello.java");
    openFileAndWaitForAnalysisCompletion(helloJavaFile);

    var defaultEditor = new DefaultEditor();
    waitForMarkers(defaultEditor,
      tuple("Replace this use of System.out by a logger.", 5));

    assertThat(new SonarLintConsole().getConsoleView().getConsoleText())
      .contains("[FileSystemSynchronizer#visitDeltaPostChange] No exclusions calculated "
        + "as 'NoIndexSupport' opted out of indexing based on other Eclipse plug-ins!");
  }

  @Test
  @Ignore("SLE-847")
  public void shouldAnalyseJavaJunit() {
    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("java/java-junit", "java-junit");

    var preferenceDialog = openPreferenceDialog();
    var preferences = new SonarLintPreferences(preferenceDialog);
    preferenceDialog.select(preferences);
    preferences.setTestFileRegularExpressions("**/*TestUtil*");
    preferenceDialog.ok();

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "hello", "Hello.java"));

    var defaultEditor = new DefaultEditor();
    waitForMarkers(defaultEditor,
      tuple("Replace this use of System.out by a logger.", 12),
      tuple("Remove this unnecessary cast to \"int\".", 16)); // Test that sonar.java.libraries is set

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "hello", "HelloTestUtil.java"));

    defaultEditor = new DefaultEditor();
    waitForMarkers(defaultEditor,
      tuple("Remove this use of \"Thread.sleep()\".", 11));

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("tests", "hello", "HelloTest.java"));

    defaultEditor = new DefaultEditor();
    waitForMarkers(defaultEditor,
      tuple("Either add an explanation about why this test is skipped or remove the \"@Ignore\" annotation.", 10),
      tuple("Add at least one assertion to this test case.", 10));
  }

  @Test
  public void shouldAnalyseJava8() {
    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("java/java8", "java8");

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "hello", "Hello.java"));

    var defaultEditor = new DefaultEditor();
    waitForMarkers(defaultEditor,
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
    waitForMarkers(defaultEditor,
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
    waitForMarkers(defaultEditor,
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
    waitForMarkers(defaultEditor,
      tuple("This loop's stop condition tests \"i\" but the incrementer updates \"j\".", 2));
  }

  // SONARIDE-349
  // SONARIDE-350
  // SONARIDE-353
  @Test
  public void shouldAnalyseJavaWithDependentProject() {
    new JavaPerspective().open();
    importExistingProjectIntoWorkspace("java/java-dependent-projects/java-dependent-project", false);
    var rootProject = importExistingProjectIntoWorkspace("java/java-dependent-projects/java-main-project", "java-main-project");

    var toBeDeleted = new File(ResourcesPlugin.getWorkspace().getRoot().getProject("java-main-project").getLocation().toFile(), "libs/toBeDeleted.jar");
    assertThat(toBeDeleted.delete()).as("Unable to delete JAR to test SONARIDE-350").isTrue();

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "use", "UseUtils.java"));

    var defaultEditor = new DefaultEditor();
    waitForMarkers(defaultEditor,
      tuple("Remove this unnecessary cast to \"int\".", 9)); // Test that sonar.java.libraries is set on dependent project
  }

  @Test
  public void shouldAnalysePython() {
    var rootProject = importExistingProjectIntoWorkspace("python", "python");
    rootProject.getTreeItem().select();
    doAndWaitForSonarLintAnalysisJob(() -> open(rootProject.getResource("src", "root", "nested", "example.py")));

    var defaultEditor = new DefaultEditor();
    waitForMarkers(defaultEditor,
      tuple("Merge this if statement with the enclosing one.", 9),
      tuple("Replace print statement by built-in function.", 10),
      tuple("Replace \"<>\" by \"!=\".", 9));
  }

  // Need PDT
  @Test
  public void shouldAnalysePHP() {
    var rootProject = importExistingProjectIntoWorkspace("php", "php");

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("foo.php"));

    var onTheFlyView = new OnTheFlyView();
    onTheFlyView.open();
    waitForSonarLintMarkers(onTheFlyView,
      tuple("Add a new line at the end of this file.", "foo.php", "few seconds ago"),
      tuple("Remove the useless trailing whitespaces at the end of this line.", "foo.php", "few seconds ago"),
      tuple("Remove this closing tag \"?>\".", "foo.php", "few seconds ago"),
      tuple("Replace \"require\" with \"require_once\".", "foo.php", "few seconds ago"),
      tuple("Replace \"require\" with namespace import mechanism through the \"use\" keyword.", "foo.php", "few seconds ago"),
      tuple("This branch duplicates the one on line 5. [+1 location]", "foo.php", "few seconds ago"),
      tuple("Remove the parentheses from this \"require\" call.", "foo.php", "few seconds ago"));

    // SLE-342
    openFileAndWaitForAnalysisCompletion(rootProject.getResource("foo.inc"));

    waitForSonarLintMarkers(onTheFlyView,
      tuple("Add a new line at the end of this file.", "foo.inc", "few seconds ago"),
      tuple("Remove the useless trailing whitespaces at the end of this line.", "foo.inc", "few seconds ago"),
      tuple("Remove this closing tag \"?>\".", "foo.inc", "few seconds ago"),
      tuple("This branch duplicates the one on line 5. [+1 location]", "foo.inc", "few seconds ago"));
  }

  @Test
  public void shouldAnalyseLinkedFile() throws IOException {
    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("java/java-linked", "java-linked");

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "hello", "HelloLinked.java"));

    // We wait for the two markers where one is complaining that the file is not actually in the package but linked
    var defaultEditor = new DefaultEditor("HelloLinked.java");
    Awaitility.await()
      .atMost(20, TimeUnit.SECONDS)
      .untilAsserted(() -> assertThat(defaultEditor.getMarkers())
        .filteredOn(marker -> marker.getType().equals("org.sonarlint.eclipse.onTheFlyIssueAnnotationType"))
        .hasSize(2));
  }

  // Need RSE
  @Test
  public void shouldAnalyseVirtualProject() throws Exception {
    // INFO: It is flaky when running on top of the oldest Eclipse version but works fine in the other test cases,
    // therefore it should be skipped in that particular situation!
    Assume.assumeTrue(!"oldest-java-11_e417".equals(System.getProperty("target.platform")));

    var remoteProjectDir = tempFolder.newFolder();
    FileUtils.copyDirectory(new File(projectDirectory, "java/java-simple"), remoteProjectDir);

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
    waitForMarkers(defaultEditor,
      tuple("Replace this use of System.out by a logger.", 9));
  }

  @Test
  public void test_CaYC_Standalone_Mode() {
    new JavaPerspective().open();
    importExistingProjectIntoWorkspace("cayc/devoxx", false);

    // Use package explorer to wait for module 1 since reddeer doesn't support hierarchical layout of project explorer
    // https://github.com/eclipse/reddeer/issues/2161
    var packageExplorer = new PackageExplorerPart();
    new WaitUntil(new ProjectExists("devoxx", packageExplorer));
    var project = packageExplorer.getProject("devoxx");

    openFileAndWaitForAnalysisCompletion(
      project.getResource("src/main/java", "devoxx", "QuizzUnreachableConditionalBranch.java"));

    // 1) Check that old markers exists
    // because the date will increase every year by one year, we cannot check the On-The-Fly view directly
    var description = "Remove this expression which always evaluates to \"false\"";
    var matcher = new MarkerDescriptionMatcher(
      CoreMatchers.containsString(description));

    var textEditor = new TextEditor();
    waitForMarkers(textEditor,
      tuple(description, 10));
    var onTheFlyView = new OnTheFlyView();
    onTheFlyView.open();

    await().untilAsserted(() -> {
      assertThat(onTheFlyView.getIssues(matcher)).hasSize(1);
      assertThat(onTheFlyView.getIssues(matcher).get(0).getCreationDate())
        .contains("years ago");
      ;
    });
    textEditor.close();

    // 2) Open project properties page and check New Code header
    project.select();
    var dialog = new PropertyDialog(project.getName());
    dialog.open();
    var sonarLintProjectProperties = new SonarLintProperties(dialog);
    dialog.select(sonarLintProjectProperties);
    await().untilAsserted(() -> {
      assertThat(sonarLintProjectProperties.newCodeHeader().getText())
        .isEqualTo("Focus on New Code is disabled");
    });
    dialog.ok();

    // 3) Enable focus on New Code
    setFocusOnNewCode(true);

    // 4) Check that old markers are gone, create new one to validate
    openFileAndWaitForAnalysisCompletion(
      project.getResource("src/main/java", "devoxx", "QuizzUnreachableConditionalBranch.java"));

    final var javaEditor = new TextEditor();
    waitForNoMarkers(javaEditor);
    waitForNoSonarLintMarkers(onTheFlyView);

    javaEditor.insertText(1, 0, "// TODO: Hello World");
    doAndWaitForSonarLintAnalysisJob(() -> javaEditor.save());
    waitForMarkers(javaEditor,
      tuple("Complete the task associated to this TODO comment.", 2));
    waitForSonarLintMarkers(onTheFlyView,
      tuple("Complete the task associated to this TODO comment.", "QuizzUnreachableConditionalBranch.java", "few seconds ago"));

    // 5) Open project properties page and check New Code header
    project.select();
    dialog = new PropertyDialog(project.getName());
    dialog.open();
    var sonarLintProjectProperties2 = new SonarLintProperties(dialog);
    dialog.select(sonarLintProjectProperties2);
    await().untilAsserted(() -> {
      assertThat(sonarLintProjectProperties2.newCodeHeader().getText())
        .isEqualTo("Focus on New Code is enabled");
    });
    dialog.ok();
  }
}
