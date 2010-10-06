package org.sonar.ide.eclipse;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SonarServerManagerTest {
  @Test
  public void test() throws Exception {
    SonarServerManager serverManager = SonarPlugin.getServerManager();
    assertThat(serverManager.getServers().size(), is(0));

    serverManager.createServer("http://localhost:9000");
    assertThat(serverManager.getServers().size(), is(1));
  }
}
