package org.sonar.ide.eclipse.tests.common;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.sonar.ide.api.Logs;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.test.AbstractSonarIdeTest;
import org.sonar.ide.test.SonarTestServer;
import org.sonar.wsclient.Host;

/**
 * Common test case for sonar-ide/eclipse projects.
 *
 * @author Jérémie Lagarde
 */
public abstract class AbstractSonarTest extends AbstractSonarIdeTest {

  protected static final IProgressMonitor monitor = new NullProgressMonitor();
  protected static IWorkspace workspace;
  protected static SonarPlugin plugin;
  private static SonarTestServer testServer;
  private List<MarkerChecker> markerCheckerList;

  @BeforeClass
  final static public void prepareWorkspace() throws Exception {
    init();

    workspace = ResourcesPlugin.getWorkspace();
    IWorkspaceDescription description = workspace.getDescription();
    description.setAutoBuilding(false);
    workspace.setDescription(description);

    plugin = SonarPlugin.getDefault();
    cleanWorkspace();
  }

  final protected String startTestServer() throws Exception {
    if (testServer == null) {
      synchronized (SonarTestServer.class) {
        if (testServer == null) {
          testServer = new SonarTestServer();
          testServer.start();
        }
      }
    }
    return testServer.getBaseUrl();
  }

  final protected String addLocalTestServer() throws Exception {
    String url = startTestServer();
    SonarPlugin.getServerManager().createServer(url);
    return url;
  }

  @AfterClass
  final static public void end() throws Exception {
    // cleanWorkspace();

    IWorkspaceDescription description = workspace.getDescription();
    description.setAutoBuilding(true);
    workspace.setDescription(description);

    if (testServer != null) {
      testServer.stop();
      testServer = null;
    }

  }

  final static private void cleanWorkspace() throws Exception {
    // Job.getJobManager().suspend();
    // waitForJobs();

    List<Host> hosts = new ArrayList<Host>();
    hosts.addAll(SonarPlugin.getServerManager().getServers());
    for (Host host : hosts) {
      SonarPlugin.getServerManager().removeServer(host.getHost());
    }
    IWorkspaceRoot root = workspace.getRoot();
    for (IProject project : root.getProjects()) {
      project.delete(true, true, monitor);
    }
  }

  /**
   * Import test project into the Eclipse workspace
   *
   * @return created projects
   */
  protected IProject importEclipseProject(String projectdir) throws IOException, CoreException {
    Logs.INFO.info("Importing Eclipse project : " + projectdir);
    IWorkspaceRoot root = workspace.getRoot();

    String projectName = projectdir;
    File dst = new File(root.getLocation().toFile(), projectName);
    dst = getProject(projectName, dst);

    final IProject project = workspace.getRoot().getProject(projectName);
    final List<IProject> addedProjectList = new ArrayList<IProject>();

    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        // create project as java project
        if (!project.exists()) {
          IProjectDescription projectDescription = workspace.newProjectDescription(project.getName());
          projectDescription.setLocation(null);
          project.create(projectDescription, monitor);
          project.open(IResource.NONE, monitor);
        } else {
          project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        }
        addedProjectList.add(project);
      }
    }, workspace.getRoot(), IWorkspace.AVOID_UPDATE, monitor);
    Logs.INFO.info("Eclipse project imported");
    return addedProjectList.get(0);
  }

  public static void waitForJobs() throws Exception {
    while (!Job.getJobManager().isIdle()) {
      Thread.sleep(1000);
    }
  }

  protected void cleanMarckerInfo() {
    markerCheckerList = null;
  }

  protected void addMarckerInfo(int priority, long line, String message) {
    if (markerCheckerList == null) {
      markerCheckerList = new ArrayList<MarkerChecker>();
    }
    markerCheckerList.add(new MarkerChecker(priority, line, message));
  }

  protected void assertMarkers(IMarker[] markers) throws CoreException {
    for (IMarker marker : markers) {
      assertMarker(marker);
    }
  }

  protected void assertMarker(IMarker marker) throws CoreException {
    if (Logs.INFO.isDebugEnabled()) {
      Logs.INFO.debug("Checker marker[" + marker.getId() + "] (" + marker.getAttribute(IMarker.PRIORITY) + ") : line " + marker.getAttribute(IMarker.LINE_NUMBER) + " : "
          + marker.getAttribute(IMarker.MESSAGE));
    }
    if (!SonarPlugin.MARKER_ID.equals(marker.getType()))
      return;
    for (MarkerChecker checker : markerCheckerList) {
      if (checker.check(marker))
        return;
    }
    fail("MarckerChecker faild for marker[" + marker.getId() + "] (" + marker.getAttribute(IMarker.PRIORITY) + ") : line " + marker.getAttribute(IMarker.LINE_NUMBER) + " : "
        + marker.getAttribute(IMarker.MESSAGE));
  }

}
