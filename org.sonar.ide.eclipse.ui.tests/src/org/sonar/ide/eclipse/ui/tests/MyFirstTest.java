package org.sonar.ide.eclipse.ui.tests;

import org.junit.Test;

public class MyFirstTest extends UITestCase {
  
  @Test
  public void canCreateANewJavaProject() throws Exception {
    importMavenProject("hello-world");
  }

}
