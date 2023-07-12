/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2023 SonarSource SA
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

import com.sonar.orchestrator.container.Server;
import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.eclipse.condition.ConsoleHasText;
import org.eclipse.reddeer.eclipse.condition.ProjectExists;
import org.eclipse.reddeer.eclipse.core.resources.Project;
import org.eclipse.reddeer.eclipse.core.resources.Resource;
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
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.impl.shell.WorkbenchShell;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.osgi.framework.Version;
import org.osgi.service.prefs.BackingStoreException;
import org.sonarlint.eclipse.its.reddeer.preferences.FileAssociationsPreferences;
import org.sonarlint.eclipse.its.reddeer.preferences.RuleConfigurationPreferences;
import org.sonarlint.eclipse.its.reddeer.views.SonarLintConsole;
import org.sonarlint.eclipse.its.reddeer.views.SonarLintConsole.ShowConsoleOption;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;

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
  private static final IEclipsePreferences ROOT = InstanceScope.INSTANCE.getNode(PLUGIN_ID);

  @ClassRule
  public static final TemporaryFolder tempFolder = new TemporaryFolder();

  @After
  public final void cleanup() {
    // first wait for previous analyzes to finish properly
    // this prevents trying to clear the console in the middle of a job
    waitSonarLintAnalysisJobs();

    var consoleView = new SonarLintConsole();
    System.out.println(consoleView.getConsoleView().getConsoleText());
    consoleView.clear();

    new WorkbenchShell().maximize();
    new CleanWorkspaceRequirement().fulfill();

    // File associations must be set explicitly on macOS!
    restoreDefaultFileAssociationConfiguration();
    restoreDefaultRulesConfiguration();

    ConfigurationScope.INSTANCE.getNode(UI_PLUGIN_ID).remove(PREF_SECRETS_EVER_DETECTED);
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
  
  @Before
  public final void before() {
    // File associations must be set explicitly on macOS!
    setSpecificFileAssociationConfiguration();
  }

  @BeforeClass
  public static final void beforeClass() throws BackingStoreException {
    System.out.println("Eclipse: " + platformVersion());
    System.out.println("GTK: " + System.getProperty("org.eclipse.swt.internal.gtk.version"));

    ROOT.node("servers").removeNode();
    ROOT_SECURE.node("servers").removeNode();

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
    consoleView.enableAnalysisLogs();
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
  }

  protected static final void importExistingProjectIntoWorkspace(String relativePathFromProjectsFolder) {
    var projectFolder = new File(projectsFolder, relativePathFromProjectsFolder);
    try {
      FileUtils.copyDirectory(new File("projects", relativePathFromProjectsFolder), projectFolder);
      var gitFolder = new File(projectFolder, "git");
      if (gitFolder.exists()) {
        FileUtils.moveDirectory(gitFolder, new File(projectFolder, ".git"));
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    var dialog = new ExternalProjectImportWizardDialog();
    dialog.open();
    var importPage = new WizardProjectsImportPage(dialog);
    importPage.copyProjectsIntoWorkspace(false);
    importPage.setRootDirectory(projectFolder.getAbsolutePath());
    var projects = importPage.getProjects();
    assertThat(projects).hasSize(1);

    // Don't use dialog.finish() as in PyDev there is an extra step before waiting for the windows to be closed
    Button button = new FinishButton(dialog);
    button.click();

    try {
      var pythonNotConfiguredDialog = new DefaultShell("Python not configured");
      new PushButton(pythonNotConfiguredDialog, "Don't ask again").click();
    } catch (Exception e) {
      // Do nothing
    }

    new WaitWhile(new WindowIsAvailable(dialog), TimePeriod.LONG);
    new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
  }

  protected static final Project importExistingProjectIntoWorkspace(String relativePathFromProjectsFolder, String projectName) {
    importExistingProjectIntoWorkspace(relativePathFromProjectsFolder);
    var projectExplorer = new ProjectExplorer();
    new WaitUntil(new ProjectExists(projectName, projectExplorer));
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

  protected static WsClient newAdminWsClient(Server server) {
    return WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .url(server.getUrl())
      .credentials(Server.ADMIN_LOGIN, Server.ADMIN_PASSWORD)
      .build());
  }
  
  static void setSpecificFileAssociationConfiguration() {
    var preferencePage = FileAssociationsPreferences.open();
    preferencePage.enforceFileAssociation();
    preferencePage.ok();
  }

  void restoreDefaultFileAssociationConfiguration() {
    var preferencePage = FileAssociationsPreferences.open();
    preferencePage.resetFileAssociation();
    preferencePage.ok();
  }

  void restoreDefaultRulesConfiguration() {
    var ruleConfigurationPreferences = RuleConfigurationPreferences.open();
    ruleConfigurationPreferences.restoreDefaults();
    ruleConfigurationPreferences.ok();
  }

}
