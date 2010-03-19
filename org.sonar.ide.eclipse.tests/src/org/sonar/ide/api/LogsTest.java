package org.sonar.ide.api;

import junit.framework.TestCase;

/**
 * Test of sonar-ide logging api in eclipse plugin.
 * 
 * @link http://jira.codehaus.org/browse/SONARIDE-35
 * 
 * @author Jérémie Lagarde
 * 
 */
public class LogsTest extends TestCase {

  protected void setUp() throws Exception {
    super.setUp();
  }

  public void testInfo() {
    // TODO fix class loading for org.slf4j.Logger
    //Logs.INFO.info("test");
  }
}
