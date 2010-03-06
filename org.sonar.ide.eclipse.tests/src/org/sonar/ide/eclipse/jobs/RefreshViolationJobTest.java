package org.sonar.ide.eclipse.jobs;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.Job;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.tests.AbstractSonarTest;
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
    addLocalTestServer();
    
    for (Host  host :  SonarPlugin.getServerManager().getServers()) {
      System.out.println("server : " + host); // TODO remove sysout.
    }
    
    IProject project = importEclipseProject("projects/SimpleProject");
    Job job = new RefreshViolationJob(project);
    job.schedule();
    
  }

}
