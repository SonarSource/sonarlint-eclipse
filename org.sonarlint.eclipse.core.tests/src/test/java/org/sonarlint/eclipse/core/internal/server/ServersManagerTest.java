/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2019 SonarSource SA
 * sonarlint@sonarsource.com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.core.internal.server;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.junit.Before;
import org.junit.Test;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.sonarlint.eclipse.core.internal.server.ServersManager.AUTH_ATTRIBUTE;
import static org.sonarlint.eclipse.core.internal.server.ServersManager.ORG_ATTRIBUTE;
import static org.sonarlint.eclipse.core.internal.server.ServersManager.PASSWORD_ATTRIBUTE;
import static org.sonarlint.eclipse.core.internal.server.ServersManager.PREF_SERVERS;
import static org.sonarlint.eclipse.core.internal.server.ServersManager.URL_ATTRIBUTE;
import static org.sonarlint.eclipse.core.internal.server.ServersManager.USERNAME_ATTRIBUTE;

/**
 * To have this test pass when launched from the IDE, you have to set -pluginCustomization argument (see pom.xml).
 */
public class ServersManagerTest {

  private static final ISecurePreferences ROOT_SECURE = SecurePreferencesFactory.getDefault().node(SonarLintCorePlugin.PLUGIN_ID);
  private static final IEclipsePreferences ROOT = InstanceScope.INSTANCE.getNode(SonarLintCorePlugin.PLUGIN_ID);
  private ServersManager manager;

  @Before
  public void prepare() throws Exception {
    manager = SonarLintCorePlugin.getServersManager();
    ROOT.node(PREF_SERVERS).removeNode();
    ROOT_SECURE.node(PREF_SERVERS).removeNode();
    manager.stop();
    manager.init();
  }

  @Test
  public void roundTripUsingPublicMethods() throws Exception {
    assertThat(ROOT.nodeExists(PREF_SERVERS)).isFalse();
    assertThat(ROOT_SECURE.nodeExists(PREF_SERVERS)).isFalse();
    String id = "foo/bar";
    IServer server = manager.create(id, "http://foo", "bar", "login", "pwd", false);
    manager.addServer(server, "login", "pwd");
    assertThat(manager.getServers()).containsExactly(server);
    assertThat(manager.findById(id)).contains(server);
    assertThat(ServersManager.getUsername(server)).isEqualTo("login");
    assertThat(ServersManager.getPassword(server)).isEqualTo("pwd");
    assertThat(ROOT.nodeExists(PREF_SERVERS)).isTrue();
    assertThat(ROOT_SECURE.nodeExists(PREF_SERVERS)).isTrue();
    assertThat(ROOT.node(PREF_SERVERS).nodeExists("foo%2Fbar")).isTrue();
    assertThat(ROOT_SECURE.node(PREF_SERVERS).nodeExists("foo%2Fbar")).isTrue();
    assertThat(ROOT.node(PREF_SERVERS).node("foo%2Fbar").get(URL_ATTRIBUTE, null)).isEqualTo("http://foo");
    assertThat(ROOT.node(PREF_SERVERS).node("foo%2Fbar").getBoolean(AUTH_ATTRIBUTE, false)).isTrue();
    assertThat(ROOT.node(PREF_SERVERS).node("foo%2Fbar").get(ORG_ATTRIBUTE, null)).isEqualTo("bar");
    assertThat(ROOT_SECURE.node(PREF_SERVERS).node("foo%2Fbar").get(USERNAME_ATTRIBUTE, null)).isEqualTo("login");
    assertThat(ROOT_SECURE.node(PREF_SERVERS).node("foo%2Fbar").get(PASSWORD_ATTRIBUTE, null)).isEqualTo("pwd");

    IServer serverUpdated = manager.create(id, "http://foo2", "bar2", "login2", "pwd2", false);
    try {
      manager.addServer(serverUpdated, "login2", "pwd2");
      fail("Expected exception");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("There is already a server with id '" + id + "'");
    }

    manager.updateServer(serverUpdated, "login2", "pwd2");
    assertThat(manager.getServers()).containsExactly(server);
    assertThat(manager.findById(id)).contains(server);
    assertThat(ROOT.node(PREF_SERVERS).node("foo%2Fbar").get(URL_ATTRIBUTE, null)).isEqualTo("http://foo2");
    assertThat(ROOT.node(PREF_SERVERS).node("foo%2Fbar").getBoolean(AUTH_ATTRIBUTE, false)).isTrue();
    assertThat(ROOT.node(PREF_SERVERS).node("foo%2Fbar").get(ORG_ATTRIBUTE, null)).isEqualTo("bar2");
    assertThat(ROOT_SECURE.node(PREF_SERVERS).node("foo%2Fbar").get(USERNAME_ATTRIBUTE, null)).isEqualTo("login2");
    assertThat(ROOT_SECURE.node(PREF_SERVERS).node("foo%2Fbar").get(PASSWORD_ATTRIBUTE, null)).isEqualTo("pwd2");

    manager.removeServer(serverUpdated);
    assertThat(manager.getServers()).isEmpty();
    assertThat(ROOT.node(PREF_SERVERS).nodeExists("foo%2Fbar")).isFalse();
    assertThat(ROOT_SECURE.node(PREF_SERVERS).nodeExists("foo%2Fbar")).isFalse();
  }

  @Test
  public void should_use_defaults_from_plugin_customization() throws Exception {
    assertThat(ROOT.nodeExists(PREF_SERVERS)).isFalse();
    assertThat(manager.getServers()).hasSize(1);
    IServer iServer = manager.findById("default").get();
    assertThat(iServer.getId()).isEqualTo("default");
    assertThat(iServer.getOrganization()).isEqualTo("myOrg");
    assertThat(iServer.hasAuth()).isTrue();
    assertThat(ROOT.nodeExists(PREF_SERVERS)).isFalse();
  }

  @Test
  public void edit_defaults_from_plugin_customization() throws Exception {
    assertThat(ROOT.nodeExists(PREF_SERVERS)).isFalse();
    assertThat(manager.getServers()).hasSize(1);
    IServer iServer = manager.findById("default").get();
    assertThat(iServer.getId()).isEqualTo("default");

    manager.updateServer(manager.create("default", "http://foo2", "bar2", "toto", null, false), "toto", null);
    iServer = manager.findById("default").get();
    assertThat(iServer.getId()).isEqualTo("default");
    assertThat(iServer.getHost()).isEqualTo("http://foo2");
    assertThat(iServer.hasAuth()).isTrue();
  }

  @Test
  public void test_listeners() {
    List<IServer> removed = new ArrayList<>();
    List<IServer> changed = new ArrayList<>();
    List<IServer> added = new ArrayList<>();
    addListener(removed, changed, added);

    IServer defaultServer = manager.findById("default").get();
    manager.removeServer(defaultServer);
    assertThat(manager.getServers()).isEmpty();
    assertThat(removed).containsExactly(defaultServer);
    assertThat(changed).isEmpty();
    assertThat(added).isEmpty();

    removed.clear();

    String id = "foo/bar";
    IServer newServer = manager.create(id, "http://foo", "bar", "login", "pwd", false);
    manager.addServer(newServer, "login", "pwd");
    assertThat(removed).isEmpty();
    assertThat(changed).isEmpty();
    assertThat(added).containsExactly(newServer);

    added.clear();

    IServer serverUpdated = manager.create(id, "http://foo2", "bar2", "login2", "pwd2", false);
    manager.updateServer(serverUpdated, "login2", "pwd2");
    assertThat(removed).isEmpty();
    assertThat(changed).containsExactly(serverUpdated);
    assertThat(added).isEmpty();
  }

  // SLE-63 Emulate external changes
  @Test
  public void test_external_changes() throws Exception {
    IServer defaultServer = manager.findById("default").get();
    manager.removeServer(defaultServer);

    List<IServer> removed = new ArrayList<>();
    List<IServer> changed = new ArrayList<>();
    List<IServer> added = new ArrayList<>();
    addListener(removed, changed, added);

    ROOT.node(PREF_SERVERS).node("foo%2Fbar").put(URL_ATTRIBUTE, "http://foo");
    ROOT.node("another").removeNode();
    assertThat(removed).isEmpty();
    assertThat(changed).isEmpty();
    assertThat(added).hasSize(1);

    added.clear();

    IServer server = manager.findById("foo/bar").get();

    ROOT.node(PREF_SERVERS).node("foo%2Fbar").put(URL_ATTRIBUTE, "http://foo:9000");
    ROOT.node(PREF_SERVERS).node("foo%2Fbar").putBoolean(AUTH_ATTRIBUTE, true);
    assertThat(removed).isEmpty();
    assertThat(changed).hasSize(2);
    assertThat(added).isEmpty();

    changed.clear();

    ROOT.node(PREF_SERVERS).node("foo%2Fbar").put(ORG_ATTRIBUTE, "bar");
    assertThat(removed).isEmpty();
    assertThat(changed).hasSize(1);
    assertThat(added).isEmpty();

    changed.clear();

    assertThat(manager.getServers()).hasSize(1);
    assertThat(server.getId()).isEqualTo("foo/bar");
    assertThat(server.getHost()).isEqualTo("http://foo:9000");
    assertThat(server.getOrganization()).isEqualTo("bar");
    assertThat(server.hasAuth()).isTrue();

    ROOT.node(PREF_SERVERS).removeNode();
    assertThat(manager.getServers()).hasSize(1);
    assertThat(manager.findById("default")).isPresent();

    assertThat(removed).hasSize(1);
    assertThat(removed.get(0).getId()).isEqualTo("foo/bar");
    assertThat(changed).isEmpty();
    assertThat(added).hasSize(1);
    assertThat(added.get(0).getId()).isEqualTo("default");
  }

  @Test
  public void test_external_changes_on_existing_servers() throws Exception {
    IServer defaultServer = manager.findById("default").get();
    manager.removeServer(defaultServer);

    manager.stop();

    // Add a server entry like if it was existing previously
    ROOT.node(PREF_SERVERS).node("foo%2Fbar").put(URL_ATTRIBUTE, "http://foo");

    manager.init();

    List<IServer> removed = new ArrayList<>();
    List<IServer> changed = new ArrayList<>();
    List<IServer> added = new ArrayList<>();
    addListener(removed, changed, added);

    IServer server = manager.findById("foo/bar").get();
    assertThat(server.getId()).isEqualTo("foo/bar");
    assertThat(server.getHost()).isEqualTo("http://foo");

    ROOT.node(PREF_SERVERS).node("foo%2Fbar").put(URL_ATTRIBUTE, "http://foo:9000");
    ROOT.node(PREF_SERVERS).node("foo%2Fbar").putBoolean(AUTH_ATTRIBUTE, true);
    assertThat(removed).isEmpty();
    assertThat(changed).hasSize(2);
    assertThat(added).isEmpty();

    changed.clear();

    assertThat(manager.getServers()).hasSize(1);
    assertThat(server.getId()).isEqualTo("foo/bar");
    assertThat(server.getHost()).isEqualTo("http://foo:9000");
  }

  private void addListener(List<IServer> removed, List<IServer> changed, List<IServer> added) {
    manager.addServerLifecycleListener(new IServerLifecycleListener() {

      @Override
      public void serverRemoved(IServer server) {
        removed.add(server);
      }

      @Override
      public void serverChanged(IServer server) {
        changed.add(server);
      }

      @Override
      public void serverAdded(IServer server) {
        added.add(server);
      }
    });
  }

}
