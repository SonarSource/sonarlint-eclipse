/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.core.internal.servers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Collection;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.security.storage.EncodingUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.prefs.BackingStoreException;
import org.sonar.ide.eclipse.common.servers.ISonarServer;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;

public class SonarServerManagerTest {

  private ServersManager serversManager;

  @Before
  public void setUp() {
    serversManager = (ServersManager) SonarCorePlugin.getServersManager();
    clean(serversManager);
  }

  /**
   * For tests.
   * @param sonarServersManager
   */
  private void clean(final ServersManager sonarServersManager) {
    final IEclipsePreferences rootNode = InstanceScope.INSTANCE.getNode(SonarCorePlugin.PLUGIN_ID);
    try {
      rootNode.node(ISonarServerPreferenceConstansts.PREF_SERVERS).removeNode();
      rootNode.node(ISonarServerPreferenceConstansts.PREF_SERVERS).put(ISonarServerPreferenceConstansts.INITIALIZED, Boolean.TRUE.toString());
      rootNode.flush();
    } catch (final BackingStoreException e) {
      SonarCorePlugin.getDefault().error(e.getMessage(), e);
    }
    final Collection<ISonarServer> servers = serversManager.getServers();
    for (final ISonarServer iSonarServer : servers) {
      Version.remove(iSonarServer);
    }
  }

  @Test
  public void shouldNotCreateFakeServer() throws Exception {
    final String url = "http://new";
    final ISonarServer server = serversManager.findServer(url);
    assertThat(server, nullValue());
    assertThat(serversManager.getServers().size(), is(0));
  }

  @Test
  public void shouldUseSecureStorage() throws Exception {
    final String url = "http://secure";
    final ISonarServer server = serversManager.create(url, "tester", "secret");
    serversManager.addServer(server);

    ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault().node(ISonarServerPreferenceConstansts.PREF_SERVERS);
    securePreferences = securePreferences.node(EncodingUtils.encodeSlashes(url));
    assertThat(securePreferences.get("username", null), is("tester"));
    assertThat(securePreferences.get("password", null), is("secret"));
  }

  @After
  public void tearDown() {
    clean(serversManager);
  }

}
