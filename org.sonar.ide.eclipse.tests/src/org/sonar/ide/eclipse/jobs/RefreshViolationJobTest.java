package org.sonar.ide.eclipse.jobs;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.jobs.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.properties.ProjectProperties;
import org.sonar.ide.eclipse.tests.common.AbstractSonarTest;
import org.sonar.wsclient.Host;

/**
 * Test case for refresh violations.
 * 
 * @link http://jira.codehaus.org/browse/SONARIDE-35
 * @link http://jira.codehaus.org/browse/SONARIDE-27
 * 
 * @author Jérémie Lagarde
 * 
 */
public class RefreshViolationJobTest extends AbstractSonarTest {

  private static final Logger LOG = LoggerFactory.getLogger(RefreshViolationJobTest.class);

  public void testRefreshViolations() throws Exception {
    // start the mock sonar server.
    String url = addLocalTestServer();
    List<Host> hosts = SonarPlugin.getServerManager().getServers();
    assertTrue("There isn't server configured.", hosts != null && hosts.size() > 0);

    if (LOG.isDebugEnabled()) {
      for (Host host : hosts) {
        LOG.debug("Server : url=" + host.getHost() + " user=" + host.getUsername() + " hasPassword=" + !StringUtils.isBlank(host.getPassword()));
      }
    }
    // Import simple project
    IProject project = importEclipseProject("projects/SimpleProject/");

    // Configure the project
    ProjectProperties properties = ProjectProperties.getInstance(project);
    properties.setUrl(url);
    properties.setGroupId("test");
    properties.setArtifactId("SimpleProject");
    properties.flush();  
    LOG.debug("Project configured");

    // Retrieve violation markers
    Job job = new RefreshViolationJob(project);
    job.schedule();
    job.join();
    LOG.debug("Violation markers retrieved");

    IMarker[] markers = project.findMarkers(SonarPlugin.MARKER_ID, true, IResource.DEPTH_INFINITE);
    assertTrue("There isn't marker for the project.", markers != null && markers.length > 0);
    for (IMarker marker : markers) {
      assertTrue("Maker must have a message.",StringUtils.isNotBlank((String)marker.getAttribute(IMarker.MESSAGE)));
      LOG.debug("marker[" + marker.getId() + "] : " + marker.getAttribute(IMarker.MESSAGE));
    }

  }

}
