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
package org.sonarlint.eclipse.its.shared;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.assertj.core.groups.Tuple;
import org.awaitility.Awaitility;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.core.lookup.ShellLookup;
import org.eclipse.reddeer.eclipse.condition.ConsoleHasText;
import org.eclipse.reddeer.eclipse.condition.ProjectExists;
import org.eclipse.reddeer.eclipse.core.resources.Project;
import org.eclipse.reddeer.eclipse.core.resources.Resource;
import org.eclipse.reddeer.eclipse.selectionwizard.ImportMenuWizard;
import org.eclipse.reddeer.eclipse.ui.navigator.resources.ProjectExplorer;
import org.eclipse.reddeer.eclipse.ui.wizards.datatransfer.ExternalProjectImportWizardDialog;
import org.eclipse.reddeer.eclipse.ui.wizards.datatransfer.WizardProjectsImportPage;
import org.eclipse.reddeer.jface.condition.WindowIsAvailable;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.cleanworkspace.CleanWorkspaceRequirement;
import org.eclipse.reddeer.requirements.cleanworkspace.CleanWorkspaceRequirement.CleanWorkspace;
import org.eclipse.reddeer.requirements.closeeditors.CloseAllEditorsRequirement.CloseAllEditors;
import org.eclipse.reddeer.swt.api.Button;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.label.DefaultLabel;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.impl.editor.AbstractEditor;
import org.eclipse.reddeer.workbench.impl.editor.Marker;
import org.eclipse.reddeer.workbench.impl.shell.WorkbenchShell;
import org.eclipse.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;
import org.eclipse.reddeer.workbench.workbenchmenu.WorkbenchMenuPreferencesDialog;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.osgi.framework.Version;
import org.osgi.service.prefs.BackingStoreException;
import org.sonarlint.eclipse.its.shared.reddeer.conditions.AnalysisReady;
import org.sonarlint.eclipse.its.shared.reddeer.conditions.AnalysisReadyAfterUnready;
import org.sonarlint.eclipse.its.shared.reddeer.preferences.FileAssociationsPreferences;
import org.sonarlint.eclipse.its.shared.reddeer.preferences.RuleConfigurationPreferences;
import org.sonarlint.eclipse.its.shared.reddeer.preferences.SonarLintPreferences;
import org.sonarlint.eclipse.its.shared.reddeer.preferences.WorkbenchPreferenceDialogCustom;
import org.sonarlint.eclipse.its.shared.reddeer.views.OnTheFlyView;
import org.sonarlint.eclipse.its.shared.reddeer.views.ReportView;
import org.sonarlint.eclipse.its.shared.reddeer.views.SonarLintConsole;
import org.sonarlint.eclipse.its.shared.reddeer.views.SonarLintConsole.ShowConsoleOption;
import org.sonarlint.eclipse.its.shared.reddeer.views.SonarLintIssueMarker;
import org.sonarlint.eclipse.its.shared.reddeer.wizards.GradleProjectImportWizardDialog;
import org.sonarlint.eclipse.its.shared.reddeer.wizards.WizardGradleProjectsImportPage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@RunWith(RedDeerSuite.class)
@CleanWorkspace
@CloseAllEditors
public abstract class AbstractSonarLintTest {
  public static final String PLUGIN_ID = "org.sonarlint.eclipse.core";
  public static final String UI_PLUGIN_ID = "org.sonarlint.eclipse.ui";
  public static final String MARKER_ON_THE_FLY_ID = PLUGIN_ID + ".sonarlintOnTheFlyProblem";
  public static final String MARKER_REPORT_ID = PLUGIN_ID + ".sonarlintReportProblem";
  public static final String PREF_SKIP_CONFIRM_ANALYZE_MULTIPLE_FILES = "skipConfirmAnalyzeMultipleFiles";
  public static final String PREF_SECRETS_EVER_DETECTED = "secretsEverDetected";

  protected static final String ON_THE_FLY_ANNOTATION_TYPE = "org.sonarlint.eclipse.onTheFlyIssueAnnotationType";

  private static final ISecurePreferences ROOT_SECURE = SecurePreferencesFactory.getDefault().node(PLUGIN_ID);
  private static final IEclipsePreferences ROOT_CORE = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
  private static final IEclipsePreferences ROOT_UI = InstanceScope.INSTANCE.getNode(UI_PLUGIN_ID);

  public static File projectDirectory = new File(new File("src").getAbsoluteFile().getParentFile().getParentFile(), "projects");

  @ClassRule
  public static final TemporaryFolder tempFolder = new TemporaryFolder();

  @BeforeClass
  public static final void setUpBeforeClass() {
    System.setProperty("sonarlint.internal.disableDogfooding", "true");
    System.setProperty("sonarlint.internal.ignoreEnhancedFeature", "true");
    System.setProperty("sonarlint.internal.ignoreMissingFeature", "true");
    System.setProperty("sonarlint.internal.ignoreNoAutomaticBuildWarning", "true");
    System.setProperty("sonarlint.internal.hideVersionHint", "true");
  }

  @AfterClass
  public static final void cleanupAfterClass() {
    System.clearProperty("sonarlint.internal.ignoreEnhancedFeature");
    System.clearProperty("sonarlint.internal.ignoreMissingFeature");
    System.clearProperty("sonarlint.internal.ignoreNoAutomaticBuildWarning");
    System.clearProperty("sonarlint.internal.hideVersionHint");

    // remove warning about soon unsupported version (there can be multiple)
    if ("oldest".equals(System.getProperty("target.platform"))) {
      Optional<DefaultShell> dialog;
      do {
        dialog = shellByName("SonarQube for Eclipse - New user survey");
        dialog.ifPresent(DefaultShell::close);
      } while (dialog.isPresent());
    }

    // File associations must be set explicitly on macOS!
    restoreDefaultFileAssociationConfiguration();
  }

  @After
  public final void cleanup() {
    // first wait for previous analyzes to finish properly
    // this prevents trying to clear the console in the middle of a job
    waitSonarLintAnalysisJobs();

    shellByName("SonarQube for Eclipse - Release Notes").ifPresent(DefaultShell::close);

    var consoleView = new SonarLintConsole();
    System.out.println(consoleView.getConsoleView().getConsoleText());
    consoleView.clear();

    new WorkbenchShell().maximize();
    new CleanWorkspaceRequirement().fulfill();

    restoreDefaultRulesConfiguration();

    setFocusOnNewCode(false);

    ROOT_UI.remove(PREF_SECRETS_EVER_DETECTED);
  }

  protected static void setFocusOnNewCode(boolean focusOnNewCode) {
    var preferenceDialog = openPreferenceDialog();
    var preferences = new SonarLintPreferences(preferenceDialog);
    preferenceDialog.select(preferences);
    preferences.setFocusOnNewCode(focusOnNewCode);
    preferenceDialog.ok();
  }

  protected static void setShowAllMarkers(boolean showAllMarkers) {
    var preferenceDialog = openPreferenceDialog();
    var preferences = new SonarLintPreferences(preferenceDialog);
    preferenceDialog.select(preferences);
    preferences.setShowAllMarkers(showAllMarkers);
    preferenceDialog.ok();
  }

  private void waitSonarLintAnalysisJobs() {
    sonarlintJobFamilies.forEach(jobFamily -> {
      try {
        Job.getJobManager().join(jobFamily, null);
      } catch (Exception e) {
        System.out.println("Error while waiting jobs to finish");
        e.printStackTrace();
      }
    });
  }

  protected static int hotspotServerPort = -1;
  private static IJobChangeListener sonarlintItJobListener;
  protected static final AtomicInteger scheduledAnalysisJobCount = new AtomicInteger();
  private static final List<CountDownLatch> analysisJobCountDownLatch = new CopyOnWriteArrayList<>();
  private static File projectsFolder;
  private static final List<String> sonarlintJobFamilies = List.of(
    "org.sonarlint.eclipse.projectJob",
    "org.sonarlint.eclipse.projectsJob");

  @BeforeClass
  public static final void beforeClass() throws BackingStoreException {
    System.out.println("Eclipse: " + platformVersion());
    System.out.println("GTK: " + System.getProperty("org.eclipse.swt.internal.gtk.version"));

    // Integration tests should not open the external browser
    System.setProperty("sonarlint.internal.externalBrowser.disabled", "true");

    ROOT_CORE.node("servers").removeNode();
    ROOT_SECURE.node("servers").removeNode();

    setSpecificFileAssociationConfiguration();

    if (sonarlintItJobListener == null) {
      sonarlintItJobListener = new JobChangeAdapter() {

        @Override
        public void scheduled(IJobChangeEvent event) {
          if (isSonarLintAnalysisJob(event)) {
            System.out.println("Job scheduled: " + event.getJob().getName());
            scheduledAnalysisJobCount.incrementAndGet();
          }
        }

        @Override
        public void done(IJobChangeEvent event) {
          if (isSonarLintAnalysisJob(event)) {
            System.out.println("Job done: " + event.getJob().getName());
            analysisJobCountDownLatch.forEach(CountDownLatch::countDown);
          }
        }

        private boolean isSonarLintAnalysisJob(IJobChangeEvent event) {
          return sonarlintJobFamilies.stream().anyMatch(family -> event.getJob().belongsTo(family));
        }
      };
      Job.getJobManager().addJobChangeListener(sonarlintItJobListener);
    }

    var consoleView = new SonarLintConsole();
    consoleView.enableVerboseOutput();
    consoleView.showConsole(ShowConsoleOption.NEVER);
    if (hotspotServerPort == -1) {
      var consoleHasText = new ConsoleHasText(consoleView.getConsoleView(), "Started embedded server on port");
      new WaitUntil(consoleHasText);
      var consoleText = consoleHasText.getResult();
      var pattern = Pattern.compile(".*Started embedded server on port (\\d+).*");
      var matcher = pattern.matcher(consoleText);
      assertThat(matcher.find()).isTrue();
      hotspotServerPort = Integer.parseInt(matcher.group(1));
    }

    try {
      projectsFolder = tempFolder.newFolder();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }

    // If we have any SonarLint for Eclipse user survey, just click it away as we don't test the behavior!
    shellByName("SonarLint - New Eclipse user survey").ifPresent(DefaultShell::close);
  }

  /** Waiting for markers to disappear in the editor */
  protected void waitForNoMarkers(AbstractEditor editor) {
    var markerType = "org.sonarlint.eclipse.onTheFlyIssueAnnotationType";

    Awaitility.await()
      .atMost(20, TimeUnit.SECONDS)
      .untilAsserted(() -> {
        assertThat(editor.getMarkers())
          .filteredOn(marker -> marker.getType().equals(markerType))
          .isEmpty();
      });
  }

  /** Waiting for markers to appear in the editor */
  protected void waitForMarkers(AbstractEditor editor, Tuple... markers) {
    var markerType = "org.sonarlint.eclipse.onTheFlyIssueAnnotationType";

    Awaitility.await()
      .atMost(20, TimeUnit.SECONDS)
      .untilAsserted(() -> {
        assertThat(editor.getMarkers())
          .filteredOn(marker -> marker.getType().equals(markerType))
          .hasSize(markers.length);
        assertThat(editor.getMarkers())
          .filteredOn(marker -> marker.getType().equals(markerType))
          .extracting(Marker::getText, Marker::getLineNumber)
          .containsOnly(markers);
      });
  }

  /** Waiting for markers to disappear in the SonarLint On-The-Fly view */
  protected void waitForNoSonarLintMarkers(OnTheFlyView view) {
    Awaitility.await()
      .atMost(20, TimeUnit.SECONDS)
      .untilAsserted(() -> {
        assertThat(view.getIssues()).isEmpty();
      });
  }

  /** Waiting for markers to appear in the SonarLint On-The-Fly view */
  protected void waitForSonarLintMarkers(OnTheFlyView view, Tuple... markers) {
    Awaitility.await()
      .atMost(20, TimeUnit.SECONDS)
      .untilAsserted(() -> {
        assertThat(view.getIssues()).hasSize(markers.length);
        assertThat(view.getIssues())
          .extracting(SonarLintIssueMarker::getDescription, SonarLintIssueMarker::getResource, SonarLintIssueMarker::getCreationDate)
          .containsOnly(markers);
      });
  }

  protected void waitForSonarLintReportIssues(ReportView view, int issues) {
    Awaitility.await()
      .atMost(20, TimeUnit.SECONDS)
      .untilAsserted(() -> assertThat(view.getItems()).hasSize(issues));
  }

  protected static final void importExistingProjectIntoWorkspace(String relativePathFromProjectsFolder, boolean isGradle) {
    var projectFolder = new File(projectsFolder, relativePathFromProjectsFolder);
    try {
      // Because the tests themselves are in child modules of the parent project ("org.sonarlint.eclipse.its") and the
      // "projects" folder is present in the parent not per module, we have to locate it correctly:
      // -> source directory ($projectDir/src) -> parent ($projectDir) -> parent ($projectDir/..) -> "projects" ($projectDir/../projects)
      // -> see definition of "projectDirectory" above
      FileUtils.copyDirectory(new File(projectDirectory, relativePathFromProjectsFolder), projectFolder);

      var gitFolder = new File(projectFolder, "git");
      if (gitFolder.exists()) {
        FileUtils.moveDirectory(gitFolder, new File(projectFolder, ".git"));
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }

    ImportMenuWizard dialog;
    if (isGradle) {
      dialog = new GradleProjectImportWizardDialog();
      dialog.open();
      var importPage = new WizardGradleProjectsImportPage(dialog);
      importPage.setRootDirectory(projectFolder.getAbsolutePath());
    } else {
      dialog = new ExternalProjectImportWizardDialog();
      dialog.open();
      var importPage = new WizardProjectsImportPage(dialog);
      importPage.copyProjectsIntoWorkspace(false);
      importPage.setRootDirectory(projectFolder.getAbsolutePath());
      var projects = importPage.getProjects();
      assertThat(projects).hasSize(1);
    }

    Button button = new FinishButton(dialog);
    button.click();

    shellByName("Python not configured").ifPresent(pythonNotConfiguredDialog -> {
      new PushButton(pythonNotConfiguredDialog, "Don't ask again").click();
    });

    new WaitWhile(new WindowIsAvailable(dialog), isGradle ? TimePeriod.VERY_LONG : TimePeriod.LONG);
    new WaitWhile(new JobIsRunning(), isGradle ? TimePeriod.VERY_LONG : TimePeriod.LONG);
  }

  /**
   * Find a shell that can be missing
   * @param title
   * @return
   */
  protected static Optional<DefaultShell> shellByName(String title) {
    try {
      return Optional.of(new DefaultShell(ShellLookup.getInstance().getShell(title, TimePeriod.SHORT)));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  protected static final Project importExistingProjectIntoWorkspace(String relativePathFromProjectsFolder, String projectName) {
    return importExistingProjectIntoWorkspace(relativePathFromProjectsFolder, projectName, false);
  }

  protected static final Project importExistingGradleProjectIntoWorkspace(String relativePathFromProjectsFolder, String projectName) {
    return importExistingProjectIntoWorkspace(relativePathFromProjectsFolder, projectName, true);
  }

  protected static final Project importExistingProjectIntoWorkspace(String relativePathFromProjectsFolder, String projectName, boolean isGradle) {
    importExistingProjectIntoWorkspace(relativePathFromProjectsFolder, isGradle);
    var projectExplorer = new ProjectExplorer();
    new WaitUntil(new ProjectExists(projectName, projectExplorer), TimePeriod.getCustom(30));
    new WaitUntil(new AnalysisReady(projectName), TimePeriod.getCustom(30));
    return projectExplorer.getProject(projectName);
  }

  protected final void doAndWaitForSonarLintAnalysisJob(Runnable r) {
    var latch = new CountDownLatch(1);
    analysisJobCountDownLatch.add(latch);
    r.run();
    try {
      assertThat(latch.await(1, TimeUnit.MINUTES)).as("Timeout expired without SonarLint analysis job").isTrue();
    } catch (InterruptedException e) {
      fail("Interrupted", e);
    } finally {
      analysisJobCountDownLatch.remove(latch);
    }
  }

  protected final void openFileAndWaitForAnalysisCompletion(Resource resource) {
    doAndWaitForSonarLintAnalysisJob(() -> open(resource));
  }

  protected static void open(Resource resource) {
    // resource.open() waits 10s for the analysis to complete which is sometimes not enough
    resource.select();
    resource.getTreeItem().doubleClick();
    new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
    // Select the file again in the explorer, else sometimes the marker view does not refresh
    resource.select();
  }

  public static boolean is2020_12OrGreater() {
    return platformVersion().compareTo(new Version("4.18")) >= 0;
  }

  public static boolean is2021_06OrLess() {
    return platformVersion().compareTo(new Version("4.20")) <= 0;
  }

  protected static Version platformVersion() {
    return Platform.getBundle("org.eclipse.platform").getVersion();
  }

  static void setSpecificFileAssociationConfiguration() {
    var preferencePage = FileAssociationsPreferences.open();
    preferencePage.enforceFileAssociation();
    preferencePage.ok();
  }

  static void restoreDefaultFileAssociationConfiguration() {
    var preferencePage = FileAssociationsPreferences.open();
    preferencePage.resetFileAssociation();
    preferencePage.ok();
  }

  protected void restoreDefaultRulesConfiguration() {
    var ruleConfigurationPreferences = RuleConfigurationPreferences.open();
    ruleConfigurationPreferences.restoreDefaults();
    ruleConfigurationPreferences.ok();
  }

  /** Some tests are not able to run on macOS due to issues with Node.js and Eclipse running in different contexts */
  protected final void ignoreMacOS() {
    var ignoreMacOS = false;

    try {
      var os = System.getProperty("os.name");
      ignoreMacOS = os == null || os.toLowerCase().contains("mac");
    } catch (Exception ignored) {
      ignoreMacOS = false;
    }

    Assume.assumeFalse(ignoreMacOS);
  }

  /** On the these notifications the "content" is always the fourth label (index 3), don't ask me why! */
  protected static String getNotificationText(DefaultShell shell) {
    return new DefaultLabel(shell, 3).getText();
  }

  /** When binding project it will move to unready state before going to ready state again */
  protected static void waitForAnalysisReady(String projectName) {
    new WaitUntil(new AnalysisReadyAfterUnready(projectName), TimePeriod.getCustom(60));
  }

  // On Eclipse 2025-06, Preferences is labeled "Preferences..." which requires this Custom call
  public static WorkbenchMenuPreferencesDialog openPreferenceDialog() {
    try {
      return openPreferenceDialog(new WorkbenchPreferenceDialogCustom());
    } catch (CoreLayerException e) {
      return openPreferenceDialog(new WorkbenchPreferenceDialog());
    }
  }

  private static WorkbenchMenuPreferencesDialog openPreferenceDialog(WorkbenchMenuPreferencesDialog preferenceDialog) {
    if (!preferenceDialog.isOpen()) {
      preferenceDialog.open();
    }
    return preferenceDialog;
  }
}
