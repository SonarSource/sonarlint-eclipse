package org.sonar.ide.eclipse;

import org.eclipse.equinox.security.storage.EncodingUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.junit.Test;
import org.sonar.ide.eclipse.core.ISonarConstants;
import org.sonar.wsclient.Host;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class SonarServerManagerTest {
  @Test
  public void shouldCreateFakeServer() throws Exception {
    SonarServerManager.enableSecureStorate(false);

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
    SonarServerManager.enableSecureStorate(true);

    String url = "http://secure";
    SonarServerManager serverManager = SonarPlugin.getServerManager();
    serverManager.addServer(url, "tester", "secret");

    ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault().node(ISonarConstants.PLUGIN_ID);
    securePreferences = securePreferences.node(EncodingUtils.encodeSlashes(url));
    assertThat(securePreferences.get("username", null), is("tester"));
    assertThat(securePreferences.get("password", null), is("secret"));

    // Cleanup
    serverManager.removeServer(url);

    SonarServerManager.enableSecureStorate(true);
  }
}
