package org.sonar.ide.eclipse.tests;

import org.junit.Test;
import org.sonar.ide.eclipse.tests.common.AbstractSonarTest;


/**
 * Simple test case to start the mock sonar server.
 * @link http://jira.codehaus.org/browse/SONARIDE-35
 * 
 * @author Jérémie Lagarde
 *
 */
public class SimpleTestWithMockServerTest extends AbstractSonarTest {
  
  @Test
  public void testStartMockServer() throws Exception {
    // start the mock sonar server.
    startTestServer();
  }

}
