package org.sonar.ide.api;

import org.junit.Test;

/**
 * Test of sonar-ide logging api in eclipse plugin.
 * 
 * @link http://jira.codehaus.org/browse/SONARIDE-35
 * 
 * @author Jérémie Lagarde
 * 
 */
public class LogsTest {
  @Test
  public void testInfo() {
    Logs.INFO.info("Logger testing...");
  }
}
