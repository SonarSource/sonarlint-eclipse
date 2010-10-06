package org.sonar.ide.eclipse.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.eclipse.equinox.security.storage.EncodingUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.junit.Test;

public class SonarServerTest {
  @Test
  public void shouldUseSecureStorage() throws Exception {
    SonarServer sonarServer = new SonarServer("http://nemo.sonarsource.org/");
    sonarServer.setCredentials("tester", "secret");

    ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault().node(ISonarConstants.PLUGIN_ID);
    securePreferences = securePreferences.node(EncodingUtils.encodeSlashes(sonarServer.getServerUrl()));

    assertThat(securePreferences.get("username", null), is("tester"));
    assertThat(securePreferences.get("password", null), is("secret"));
  }
}
