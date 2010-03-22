package org.sonar.ide.eclipse.jobs;

import static org.junit.Assert.assertTrue;

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
  private IProject project;


  protected void prepareTest() throws Exception {
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
    project = importEclipseProject("projects/SimpleProject/");

    // Configure the project
    ProjectProperties properties = ProjectProperties.getInstance(project);
    properties.setUrl(url);
    properties.setGroupId("test");
    properties.setArtifactId("SimpleProject");
    properties.flush();
    LOG.debug("Project configured");
  }
    
  @Test
  public void testRefreshViolations() throws Exception {
    prepareTest();
    
    // Retrieve violation markers
    Job job = new RefreshViolationJob(project);
    job.schedule();
    job.join();
    LOG.debug("Violation markers retrieved");

    cleanMarckerInfo();
    addMarckerInfo(1,7,"Unused private method : Avoid unused private methods such as 'myMethod()'.");
    addMarckerInfo(0,4,"Unused local variable : Avoid unused local variables such as 'j'.");
    addMarckerInfo(1,4,"Parameter Assignment : Assignment of parameter 'i' is not allowed.");
    addMarckerInfo(2,1,"Hide Utility Class Constructor : Utility classes should not have a public or default constructor.");
    
    IMarker[] markers = project.findMarkers(SonarPlugin.MARKER_ID, true, IResource.DEPTH_INFINITE);
    assertTrue("There isn't marker for the project.", markers != null && markers.length > 0);
    assertMarkers(markers);
  }
  
  @Test
  public void testRefreshViolationsWithModifiedFile() throws Exception {
    prepareTest();
    
    // Add blank line.
    IJavaProject javaProject = JavaCore.create(project);
    ICompilationUnit unit = (ICompilationUnit)javaProject.findElement(new Path("ClassOnDefaultPackage.java"));
    IBuffer buffer = unit.getBuffer();
    String content = buffer.getContents();
    content = StringUtils.replace(content, "  private String myMethod()", "\n  private String myMethod()");
    buffer.setContents(content);
    buffer.save(new NullProgressMonitor(), true);
    
    // Retrieve violation markers
    Job job = new RefreshViolationJob(project);
    job.schedule();
    job.join();
    LOG.debug("Violation markers retrieved");

    cleanMarckerInfo();
    addMarckerInfo(1,8,"Unused private method : Avoid unused private methods such as 'myMethod()'.");
    addMarckerInfo(0,4,"Unused local variable : Avoid unused local variables such as 'j'.");
    addMarckerInfo(1,4,"Parameter Assignment : Assignment of parameter 'i' is not allowed.");
    addMarckerInfo(2,1,"Hide Utility Class Constructor : Utility classes should not have a public or default constructor.");
    
    IMarker[] markers = project.findMarkers(SonarPlugin.MARKER_ID, true, IResource.DEPTH_INFINITE);
    assertTrue("There isn't marker for the project.", markers != null && markers.length > 0);
    assertMarkers(markers);
  }

}
