/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2020 SonarSource SA
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
import java.io.IOException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.iterable.Extractor;
import org.assertj.core.groups.Tuple;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.BoolResult;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.osgi.framework.Version;
import org.sonarlint.eclipse.its.bots.ConsoleViewBot;
import org.sonarlint.eclipse.its.utils.CaptureScreenshotAndConsoleOnFailure;
import org.sonarlint.eclipse.its.utils.SwtBotUtils;
import org.sonarlint.eclipse.its.utils.WorkspaceHelpers;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;

import static org.junit.Assert.assertTrue;

public abstract class AbstractSonarLintTest {

  public static final String PLUGIN_ID = "org.sonarlint.eclipse.core";
  public static final String UI_PLUGIN_ID = "org.sonarlint.eclipse.ui";
  public static final String MARKER_ON_THE_FLY_ID = PLUGIN_ID + ".sonarlintOnTheFlyProblem";
  public static final String MARKER_REPORT_ID = PLUGIN_ID + ".sonarlintReportProblem";

  protected static SWTWorkbenchBot bot;

  @Rule
  public CaptureScreenshotAndConsoleOnFailure screenshot = new CaptureScreenshotAndConsoleOnFailure();

  protected static final IProgressMonitor monitor = new NullProgressMonitor();
  protected static IWorkspace workspace;
  protected static File projectsWorkdir;
  private static final ReadWriteLock copyProjectLock = new ReentrantReadWriteLock();

  private static final ISecurePreferences ROOT_SECURE = SecurePreferencesFactory.getDefault().node(PLUGIN_ID);
  private static final IEclipsePreferences ROOT = InstanceScope.INSTANCE.getNode(PLUGIN_ID);

  @BeforeClass
  public final static void beforeClass() throws Exception {
    System.out.println("Eclipse: " + platformVersion());
    System.out.println("GTK: " + System.getProperty("org.eclipse.swt.internal.gtk.version"));
    SWTBotPreferences.KEYBOARD_LAYOUT = "EN_US";

    projectsWorkdir = new File("target/projects-target");

    workspace = ResourcesPlugin.getWorkspace();

    ROOT.node("servers").removeNode();
    ROOT_SECURE.node("servers").removeNode();

    bot = new SWTWorkbenchBot();

    SwtBotUtils.closeViewQuietly(bot, "org.eclipse.ui.internal.introview");
    SwtBotUtils.closeViewQuietly(bot, "org.eclipse.ui.views.ContentOutline");
    SwtBotUtils.closeViewQuietly(bot, "org.eclipse.mylyn.tasks.ui.views.tasks");

    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);

    new ConsoleViewBot(bot)
      .openSonarLintConsole()
      .enableVerboseLogs();
  }

  @AfterClass
  public final static void afterClass() throws Exception {
    try {
      clean();
    } catch (Exception e) {
      // Silently ignore exceptions at this point
      System.err.println("[WARN] Error during cleanup: " + e.getMessage());
    }
  }

  private static void clean() throws InterruptedException, CoreException {
    WorkspaceHelpers.cleanWorkspace(bot);
    bot.resetWorkbench();
  }

  public static String getProjectPath(String name) throws IOException {
    return getProject(name).getCanonicalPath();
  }

  protected static File getProject(String projectName) throws IOException {
    File destDir = new File(projectsWorkdir, projectName);
    return getProject(projectName, destDir);
  }

  /**
   * Installs specified project to specified directory.
   *
   * @param projectName
   *          name of project
   * @param destDir
   *          destination directory
   * @return project directory
   * @throws IOException
   *           if unable to prepare project directory
   */
  protected static File getProject(String projectdir, File destDir) throws IOException {
    copyProjectLock.writeLock().lock();
    try {
      File projectFolder = new File("projects", projectdir);
      assertTrue("Project " + projectdir + " folder not found.\n" + projectFolder.getAbsolutePath(), projectFolder.isDirectory());
      FileUtils.copyDirectory(projectFolder, destDir);
      return destDir;
    } finally {
      copyProjectLock.writeLock().unlock();
    }
  }

  /**
   * Import test project into the Eclipse workspace
   *
   * @return created projects
   */
  public static IProject importEclipseProject(final String projectdir, final String projectName) throws IOException, CoreException {
    final IWorkspaceRoot root = workspace.getRoot();

    File dst = new File(root.getLocation().toFile(), projectName);
    getProject(projectdir, dst);

    final IProject project = workspace.getRoot().getProject(projectName);

    workspace.run(new IWorkspaceRunnable() {

      @Override
      public void run(final IProgressMonitor monitor) throws CoreException {
        if (!project.exists()) {
          final IProjectDescription projectDescription = workspace.newProjectDescription(project.getName());
          projectDescription.setLocation(null);
          project.create(projectDescription, monitor);
          project.open(IResource.NONE, monitor);
        } else {
          project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        }
      }
    }, workspace.getRoot(), IWorkspace.AVOID_UPDATE, monitor);

    return project;
  }

  public static MarkerAttributesExtractor markerAttributes(String... attributes) {
    return new MarkerAttributesExtractor(attributes);
  }

  public static class MarkerAttributesExtractor implements Extractor<IMarker, Tuple> {

    private final String[] attributes;

    public MarkerAttributesExtractor(String... attributes) {
      this.attributes = attributes;
    }

    @Override
    public Tuple extract(IMarker marker) {
      Object[] tupleAttributes = new Object[attributes.length + 1];
      tupleAttributes[0] = marker.getResource().getFullPath().toPortableString();
      for (int i = 0; i < attributes.length; i++) {
        try {
          tupleAttributes[i + 1] = marker.getAttribute(attributes[i]);
        } catch (CoreException e) {
          throw new IllegalStateException("Unable to get attribute '" + attributes[i] + "'");
        }
      }
      return new Tuple(tupleAttributes);
    }
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

  public void waitForServerUpdate(String serverName, Orchestrator orch, boolean isSonarCloud) {
    SWTBotView serversView = bot.viewById("org.sonarlint.eclipse.ui.ServersView");
    final SWTBotTreeItem serverCell = serversView.bot().tree().getAllItems()[0];
    bot.waitUntil(new DefaultCondition() {
      @Override
      public boolean test() throws Exception {
        return UIThreadRunnable.syncExec(new BoolResult() {
          @Override
          public Boolean run() {
            return serverCell.getText().matches(serverName + " \\[" +
              (isSonarCloud ? "" : "Version: " + substringBefore(orch.getServer().version(), "-") + "(.*), ")
              + "Last storage update: (.*)\\]");
          }

        });
      };

      @Override
      public String getFailureMessage() {
        return "Server status is: " + serverCell.getText();
      }
    }, 20_000);
  }

  private String substringBefore(com.sonar.orchestrator.version.Version version, String string) {
    int indexOfDash = string.indexOf("-");
    if (indexOfDash == -1) {
      return string;
    }
    return string.substring(0, indexOfDash);
  }

}
