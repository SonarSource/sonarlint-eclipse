package org.sonar.ide.eclipse;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.eclipse.equinox.security.storage.EncodingUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.ide.eclipse.core.SonarCorePlugin;
import org.sonar.ide.eclipse.internal.core.ServersManager;
import org.sonar.wsclient.Host;

public class SonarServerManagerTest {

  private ServersManager serversManager;

  @Before
  public void setUp() {
    serversManager = (ServersManager) SonarCorePlugin.getServersManager();
    serversManager.clean();
  }

  @Test
  public void shouldCreateFakeServer() throws Exception {
    String url = "http://new";
    Host host = serversManager.findServer(url);
    assertThat(host, notNullValue());
    assertThat(serversManager.getServers().size(), is(1));
  }

  @Test
  public void shouldUseSecureStorage() throws Exception {
    String url = "http://secure";
    serversManager.addServer(url, "tester", "secret");

    ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault().node(ServersManager.NODE);
    securePreferences = securePreferences.node(EncodingUtils.encodeSlashes(url));
    assertThat(securePreferences.get("username", null), is("tester"));
    assertThat(securePreferences.get("password", null), is("secret"));
  }

  @After
  public void tearDown() {
    serversManager.clean();
  }

}
