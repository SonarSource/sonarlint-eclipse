/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

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
