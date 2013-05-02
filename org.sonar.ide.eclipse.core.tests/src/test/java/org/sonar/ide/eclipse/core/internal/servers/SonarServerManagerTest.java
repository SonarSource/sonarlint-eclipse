/*
 * Sonar Eclipse
 * Copyright (C) 2010-2013 SonarSource
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

import org.eclipse.equinox.security.storage.EncodingUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.ide.eclipse.common.servers.ISonarServer;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class SonarServerManagerTest {

  private ServersManager serversManager;

  @Before
  public void setUp() {
    serversManager = (ServersManager) SonarCorePlugin.getServersManager();
    serversManager.clean();
  }

  @Test
  public void shouldNotCreateFakeServer() throws Exception {
    String url = "http://new";
    ISonarServer server = serversManager.findServer(url);
    assertThat(server, nullValue());
    assertThat(serversManager.getServers().size(), is(0));
  }

  @Test
  public void shouldUseSecureStorage() throws Exception {
    String url = "http://secure";
    serversManager.addServer(url, "tester", "secret");

    ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault().node(ServersManager.PREF_SERVERS);
    securePreferences = securePreferences.node(EncodingUtils.encodeSlashes(url));
    assertThat(securePreferences.get("username", null), is("tester"));
    assertThat(securePreferences.get("password", null), is("secret"));
  }

  @After
  public void tearDown() {
    serversManager.clean();
  }

}
