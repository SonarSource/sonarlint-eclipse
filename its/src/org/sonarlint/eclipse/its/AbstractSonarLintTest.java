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

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.container.Server;
import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.eclipse.reddeer.eclipse.condition.ConsoleHasText;
import org.eclipse.reddeer.eclipse.condition.ProjectExists;
import org.eclipse.reddeer.eclipse.core.resources.Project;
import org.eclipse.reddeer.eclipse.core.resources.Resource;
import org.eclipse.reddeer.eclipse.ui.navigator.resources.ProjectExplorer;
import org.eclipse.reddeer.eclipse.ui.wizards.datatransfer.ExternalProjectImportWizardDialog;
import org.eclipse.reddeer.eclipse.ui.wizards.datatransfer.WizardProjectsImportPage;
import org.eclipse.reddeer.eclipse.ui.wizards.datatransfer.WizardProjectsImportPage.ImportProject;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.cleanworkspace.CleanWorkspaceRequirement;
import org.eclipse.reddeer.requirements.cleanworkspace.CleanWorkspaceRequirement.CleanWorkspace;
import org.eclipse.reddeer.requirements.closeeditors.CloseAllEditorsRequirement.CloseAllEditors;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.osgi.framework.Version;
import org.osgi.service.prefs.BackingStoreException;
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

  private static final ISecurePreferences ROOT_SECURE = SecurePreferencesFactory.getDefault().node(PLUGIN_ID);
  private static final IEclipsePreferences ROOT = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
  private static SonarLintConsole consoleView;

  @After
  public void cleanup() {
    WorkbenchPreferenceDialog preferenceDialog = new WorkbenchPreferenceDialog();
    if (preferenceDialog.isOpen()) {
      preferenceDialog.cancel();
    }

    new CleanWorkspaceRequirement().fulfill();
  }

  protected static int hotspotServerPort = -1;
  private static IJobChangeListener sonarlintItJobListener;
  protected static final AtomicInteger scheduledAnalysisJobCount = new AtomicInteger();
  private static final List<CountDownLatch> analysisJobCountDownLatch = new CopyOnWriteArrayList<>();

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
            analysisJobCountDownLatch.forEach(l -> l.countDown());
          }
        }

        private boolean isSonarLintAnalysisJob(IJobChangeEvent event) {
          return event.getJob().belongsTo("org.sonarlint.eclipse.projectJob") || event.getJob().belongsTo("org.sonarlint.eclipse.projectsJob");
        }
      };
      Job.getJobManager().addJobChangeListener(sonarlintItJobListener);
    }

    if (consoleView == null) {
      consoleView = new SonarLintConsole();
      consoleView.open();
      consoleView.openConsole("SonarLint");
      consoleView.enableAnalysisLogs();
      consoleView.showConsole(ShowConsoleOption.NEVER);
      new WaitUntil(new ConsoleHasText(consoleView, "Started security hotspot handler on port"));
      String consoleText = consoleView.getConsoleText();
      Pattern p = Pattern.compile(".*Started security hotspot handler on port (\\d+).*");
      Matcher m = p.matcher(consoleText);
      assertThat(m.find()).isTrue();
      hotspotServerPort = Integer.parseInt(m.group(1));
    }
  }

  protected static final void importExistingProjectIntoWorkspace(String relativePathFromProjectsFolder) {
    ExternalProjectImportWizardDialog dialog = new ExternalProjectImportWizardDialog();
    dialog.open();
    WizardProjectsImportPage importPage = new WizardProjectsImportPage(dialog);
    importPage.copyProjectsIntoWorkspace(true);
    importPage.setRootDirectory(new File("projects", relativePathFromProjectsFolder).getAbsolutePath());
    List<ImportProject> projects = importPage.getProjects();
    assertThat(projects).hasSize(1);
    dialog.finish();
  }

  protected static final Project importExistingProjectIntoWorkspace(String relativePathFromProjectsFolder, String projectName) {
    importExistingProjectIntoWorkspace(relativePathFromProjectsFolder);
    ProjectExplorer projectExplorer = new ProjectExplorer();
    new WaitUntil(new ProjectExists(projectName, projectExplorer));
    return projectExplorer.getProject(projectName);
  }

  protected final void doAndWaitForSonarLintAnalysisJob(Runnable r) {
    CountDownLatch latch = new CountDownLatch(1);
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

  /**
   * JavaSE-1.8 was added in Kepler SR2 / Luna
   */
  protected boolean supportJava8() {
    return platformVersion().compareTo(new Version("4.4")) >= 0;
  }

  /**
   * JUnit was shipped in ???
   */
  protected boolean supportJunit() {
    return platformVersion().compareTo(new Version("4.4")) >= 0;
  }

  public static boolean isPhotonOrGreater() {
    return platformVersion().compareTo(new Version("4.8")) >= 0;
  }

  public static boolean isOxygenOrGreater() {
    return platformVersion().compareTo(new Version("4.7")) >= 0;
  }

  public static boolean isMarsOrGreater() {
    return platformVersion().compareTo(new Version("4.5")) >= 0;
  }

  public static boolean isNeonOrGreater() {
    return platformVersion().compareTo(new Version("4.6")) >= 0;
  }

  public static boolean is2020_12OrGreater() {
    return platformVersion().compareTo(new Version("4.18")) >= 0;
  }

  protected static Version platformVersion() {
    return Platform.getBundle("org.eclipse.platform").getVersion();
  }

  protected static WsClient newAdminWsClient(Orchestrator orchestrator) {
    Server server = orchestrator.getServer();
    return WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .url(server.getUrl())
      .credentials(Server.ADMIN_LOGIN, Server.ADMIN_PASSWORD)
      .build());
  }

}
