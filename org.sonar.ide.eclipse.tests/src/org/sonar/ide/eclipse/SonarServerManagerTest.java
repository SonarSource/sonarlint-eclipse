package org.sonar.ide.eclipse;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.eclipse.equinox.security.storage.EncodingUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.junit.Test;
import org.sonar.ide.eclipse.core.ISonarConstants;
import org.sonar.wsclient.Host;

public class SonarServerManagerTest {
  @Test
  public void shouldCreateFakeServer() throws Exception {
    String url = "http://new";
    SonarServerManager serverManager = SonarPlugin.getServerManager();

    assertThat(serverManager.getServers().size(), is(0));

    Host host = serverManager.findServer(url);
    assertThat(host, notNullValue());
    assertThat(serverManager.getServers().size(), is(1));

    // Cleanup
    serverManager.removeServer(url);
  }

  @Test
  public void shouldUseSecureStorage() throws Exception {
    String url = "http://secure";
    SonarServerManager serverManager = SonarPlugin.getServerManager();
    serverManager.addServer(url, "tester", "secret");

    ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault().node(ISonarConstants.PLUGIN_ID);
    securePreferences = securePreferences.node(EncodingUtils.encodeSlashes(url));
    assertThat(securePreferences.get("username", null), is("tester"));
    assertThat(securePreferences.get("password", null), is("secret"));

    // Cleanup
    serverManager.removeServer(url);
  }
}
