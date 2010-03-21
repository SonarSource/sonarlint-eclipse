package org.sonar.ide.api;

import junit.framework.TestCase;

/**
 * Test of sonar-ide excpetion api in eclipse plugin.
 * 
 * @link http://jira.codehaus.org/browse/SONARIDE-35
 * 
 * @author Jérémie Lagarde
 * 
 */
public class SonarIdeExceptionTest extends TestCase {

  public void testException() {
    String msg = "testing ...";
    try {
      throwSonarIdeException(msg);
      fail();
    } catch (SonarIdeException e) {
      assertEquals(msg, e.getMessage());
    }
  }

  public void testExceptionWithCause() {
    String msg = "testing ...";
    try {
      throwSonarIdeExceptionWithCause(msg);
      fail();
    } catch (SonarIdeException e) {
      assertEquals(msg, e.getMessage());
      assertNotNull(e.getCause());
    }
  }

  private void throwSonarIdeException(String msg) {
    throw new SonarIdeException(msg);
  }

  private void throwSonarIdeExceptionWithCause(String msg) {
    throw new SonarIdeException(msg, new NullPointerException());
  }
}
