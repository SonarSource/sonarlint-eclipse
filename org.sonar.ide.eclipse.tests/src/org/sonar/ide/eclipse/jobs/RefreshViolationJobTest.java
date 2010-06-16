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

package org.sonar.ide.eclipse.jobs;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.properties.ProjectProperties;
import org.sonar.ide.eclipse.tests.common.SonarTestCase;
import org.sonar.wsclient.Host;

/**
 * Test case for refresh violations.
 * 
 * @author Jérémie Lagarde
 * @link http://jira.codehaus.org/browse/SONARIDE-35
 * @link http://jira.codehaus.org/browse/SONARIDE-27
 */
public class RefreshViolationJobTest extends SonarTestCase {

  private static final Logger LOG = LoggerFactory.getLogger(RefreshViolationJobTest.class);
  private IProject project;

  protected void prepareTest() throws Exception {
    // start the mock sonar server.
    final String url = addLocalTestServer();
    final List<Host> hosts = SonarPlugin.getServerManager().getServers();
    assertTrue("There isn't server configured.", hosts != null && hosts.size() > 0);

    if (LOG.isDebugEnabled()) {
      for (final Host host : hosts) {
        LOG.debug("Server : url=" + host.getHost() + " user=" + host.getUsername() + " hasPassword="
            + !StringUtils.isBlank(host.getPassword()));
      }
    }
    // Import simple project
    project = importEclipseProject("SimpleProject");

    // Configure the project
    final ProjectProperties properties = ProjectProperties.getInstance(project);
    properties.setUrl(url);
    properties.setGroupId("org.sonar-ide.tests.SimpleProject");
    properties.setArtifactId("SimpleProject");
    properties.save();
    LOG.debug("Project configured");
  }

  @Test
  public void testRefreshViolations() throws Exception {
    prepareTest();

    // Retrieve violation markers
    final List<IResource> resources = new ArrayList<IResource>();
    resources.add(project);
    final Job job = new RefreshViolationJob(resources);
    job.schedule();
    job.join();
    LOG.debug("Violation markers retrieved");

    cleanMarckerInfo();
    addMarckerInfo(1, 7, "Unused private method : Avoid unused private methods such as 'myMethod()'.");
    addMarckerInfo(0, 4, "Unused local variable : Avoid unused local variables such as 'j'.");
    addMarckerInfo(1, 4, "Parameter Assignment : Assignment of parameter 'i' is not allowed.");
    addMarckerInfo(2, 1, "Hide Utility Class Constructor : Utility classes should not have a public or default constructor.");

    final IMarker[] markers = project.findMarkers(SonarPlugin.MARKER_VIOLATION_ID, true, IResource.DEPTH_INFINITE);
    assertTrue("There isn't marker for the project.", markers != null && markers.length > 0);
    assertMarkers(markers);
  }

  @Test
  public void testRefreshViolationsWithModifiedFile() throws Exception {
    prepareTest();

    // Add blank line.
    final IJavaProject javaProject = JavaCore.create(project);
    final ICompilationUnit unit = (ICompilationUnit) javaProject.findElement(new Path("ClassOnDefaultPackage.java"));
    final IBuffer buffer = unit.getBuffer();
    String content = buffer.getContents();
    content = StringUtils.replace(content, "  private String myMethod()", "\n  private String myMethod()");
    buffer.setContents(content);
    buffer.save(new NullProgressMonitor(), true);

    // Retrieve violation markers
    final List<IResource> resources = new ArrayList<IResource>();
    resources.add(project);
    final Job job = new RefreshViolationJob(resources);
    job.schedule();
    job.join();
    LOG.debug("Violation markers retrieved");

    cleanMarckerInfo();
    addMarckerInfo(1, 8, "Unused private method : Avoid unused private methods such as 'myMethod()'.");
    addMarckerInfo(0, 4, "Unused local variable : Avoid unused local variables such as 'j'.");
    addMarckerInfo(1, 4, "Parameter Assignment : Assignment of parameter 'i' is not allowed.");
    addMarckerInfo(2, 1, "Hide Utility Class Constructor : Utility classes should not have a public or default constructor.");

    final IMarker[] markers = project.findMarkers(SonarPlugin.MARKER_VIOLATION_ID, true, IResource.DEPTH_INFINITE);
    assertTrue("There isn't marker for the project.", markers != null && markers.length > 0);
    assertMarkers(markers);
  }

}
