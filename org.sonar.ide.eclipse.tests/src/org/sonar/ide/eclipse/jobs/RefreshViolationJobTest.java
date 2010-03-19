package org.sonar.ide.eclipse.jobs;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.jobs.Job;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.properties.ProjectProperties;
import org.sonar.ide.eclipse.tests.common.AbstractSonarTest;
import org.sonar.wsclient.Host;
/**
 * Test case for refresh violations.
 * @link http://jira.codehaus.org/browse/SONARIDE-35
 * @link http://jira.codehaus.org/browse/SONARIDE-27
 * 
 * @author Jérémie Lagarde
 *
 */
public class RefreshViolationJobTest extends AbstractSonarTest {

  

  public void testRefreshViolations() throws Exception {
    // start the mock sonar server.
    String url = addLocalTestServer();
    for (Host  host :  SonarPlugin.getServerManager().getServers()) {
      System.out.println("server : " + host.getHost()); // TODO remove sysout.
    }
    
    // Import simple project
    IProject project = importEclipseProject("projects/SimpleProject/");

    // Configure the project 
    ProjectProperties properties = ProjectProperties.getInstance(project);
    properties.setUrl(url);
    properties.setGroupId("test");
    properties.setArtifactId("SimpleProject");
    properties.flush();
    System.out.println("Project configured");
    
    // Retrieve violation markers
    Job job = new RefreshViolationJob(project);
    job.schedule();
    job.join();
    System.out.println("Violation markers retrieved");
    
    // TODO check sonar makers.
    IMarker[] markers = project.findMarkers(SonarPlugin.MARKER_ID, true, IResource.DEPTH_INFINITE);
    for (IMarker marker : markers) {
      System.out.println("   - marker : " + marker.getId() );
    }
    
  }

}
