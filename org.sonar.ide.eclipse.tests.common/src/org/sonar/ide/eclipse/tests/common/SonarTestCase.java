/*
 * Copyright (C) 2010 Evgeny Mandrikov
 *
 * Sonar-IDE is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar-IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar-IDE; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

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
import org.sonar.ide.test.SonarIdeTestCase;
import org.sonar.ide.test.SonarTestServer;
import org.sonar.wsclient.Host;

/**
 * Common test case for sonar-ide/eclipse projects.
 * 
 * @author Jérémie Lagarde
 */
public abstract class SonarTestCase extends SonarIdeTestCase {

  protected static final IProgressMonitor monitor = new NullProgressMonitor();
  protected static IWorkspace workspace;
  protected static SonarPlugin plugin;
  private static SonarTestServer testServer;
  private List<MarkerChecker> markerCheckerList;

  @BeforeClass
  final static public void prepareWorkspace() throws Exception {
    init();

    workspace = ResourcesPlugin.getWorkspace();
    final IWorkspaceDescription description = workspace.getDescription();
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
    final String url = startTestServer();
    SonarPlugin.getServerManager().createServer(url);
    return url;
  }

  @AfterClass
  final static public void end() throws Exception {
    // cleanWorkspace();

    final IWorkspaceDescription description = workspace.getDescription();
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

    final List<Host> hosts = new ArrayList<Host>();
    hosts.addAll(SonarPlugin.getServerManager().getServers());
    for (final Host host : hosts) {
      SonarPlugin.getServerManager().removeServer(host.getHost());
    }
    final IWorkspaceRoot root = workspace.getRoot();
    for (final IProject project : root.getProjects()) {
      project.delete(true, true, monitor);
    }
  }

  /**
   * Import test project into the Eclipse workspace
   * 
   * @return created projects
   */
  protected IProject importEclipseProject(final String projectdir) throws IOException, CoreException {
    Logs.INFO.info("Importing Eclipse project : " + projectdir);
    final IWorkspaceRoot root = workspace.getRoot();

    final String projectName = projectdir;
    File dst = new File(root.getLocation().toFile(), projectName);
    dst = getProject(projectName, dst);

    final IProject project = workspace.getRoot().getProject(projectName);
    final List<IProject> addedProjectList = new ArrayList<IProject>();

    workspace.run(new IWorkspaceRunnable() {

      public void run(final IProgressMonitor monitor) throws CoreException {
        // create project as java project
        if ( !project.exists()) {
          final IProjectDescription projectDescription = workspace.newProjectDescription(project.getName());
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
    while ( !Job.getJobManager().isIdle()) {
      Thread.sleep(1000);
    }
  }

  protected void cleanMarckerInfo() {
    markerCheckerList = null;
  }

  protected void addMarckerInfo(final int priority, final long line, final String message) {
    if (markerCheckerList == null) {
      markerCheckerList = new ArrayList<MarkerChecker>();
    }
    markerCheckerList.add(new MarkerChecker(priority, line, message));
  }

  protected void assertMarkers(final IMarker[] markers) throws CoreException {
    for (final IMarker marker : markers) {
      assertMarker(marker);
    }
  }

  protected void assertMarker(final IMarker marker) throws CoreException {
    if (Logs.INFO.isDebugEnabled()) {
      Logs.INFO.debug("Checker marker[" + marker.getId() + "] (" + marker.getAttribute(IMarker.PRIORITY) + ") : line "
          + marker.getAttribute(IMarker.LINE_NUMBER) + " : " + marker.getAttribute(IMarker.MESSAGE));
    }
    if (!SonarPlugin.MARKER_VIOLATION_ID.equals(marker.getType())) {
      return;
    }
    for (final MarkerChecker checker : markerCheckerList) {
      if (checker.check(marker)) {
        return;
      }
    }
    fail("MarckerChecker faild for marker[" + marker.getId() + "] (" + marker.getAttribute(IMarker.PRIORITY) + ") : line "
        + marker.getAttribute(IMarker.LINE_NUMBER) + " : " + marker.getAttribute(IMarker.MESSAGE));
  }

}
