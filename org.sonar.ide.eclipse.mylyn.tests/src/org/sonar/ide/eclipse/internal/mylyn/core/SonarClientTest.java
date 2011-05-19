package org.sonar.ide.eclipse.internal.mylyn.core;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import org.eclipse.mylyn.commons.net.AuthenticationCredentials;
import org.eclipse.mylyn.commons.net.AuthenticationType;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.junit.Test;
import org.sonar.wsclient.Host;

public class SonarClientTest {

  @Test
  public void testGetSonarHost() {
    TaskRepository repository = new TaskRepository(SonarConnector.CONNECTOR_KIND, "http://localhost:9000");
    SonarClient client = new SonarClient(repository);

    Host host = client.getSonarHost();
    assertThat(host.getHost(), is("http://localhost:9000"));
    assertThat(host.getUsername(), nullValue());
    assertThat(host.getPassword(), nullValue());

    repository.setCredentials(AuthenticationType.REPOSITORY, new AuthenticationCredentials("username", "password"), false);
    host = client.getSonarHost();
    assertThat(host.getUsername(), is("username"));
    assertThat(host.getPassword(), is("password"));
  }

}
