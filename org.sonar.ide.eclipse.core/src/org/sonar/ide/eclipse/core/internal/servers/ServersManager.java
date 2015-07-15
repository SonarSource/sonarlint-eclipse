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

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.annotation.CheckForNull;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.security.storage.EncodingUtils;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.sonar.ide.eclipse.common.servers.ISonarServer;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;

public class ServersManager implements ISonarServersManager {

  @Override
  public Collection<ISonarServer> getServers() {
    final IEclipsePreferences rootNode = InstanceScope.INSTANCE.getNode(SonarCorePlugin.PLUGIN_ID);
    final List<ISonarServer> servers = Lists.newArrayList();
    try {
      rootNode.sync();
      if (rootNode.nodeExists(ISonarServerPreferenceConstansts.PREF_SERVERS)) {
        final Preferences serversNode = rootNode.node(ISonarServerPreferenceConstansts.PREF_SERVERS);
        for (final String encodedUrl : serversNode.childrenNames()) {
          final Preferences serverNode = serversNode.node(encodedUrl);
          final String url = EncodingUtils.decodeSlashes(encodedUrl);
          final boolean auth = serverNode.getBoolean(ISonarServerPreferenceConstansts.AUTH, false);
          final Version version = Version.getVersion(url);
          final SonarServer sonarServer = new SonarServer(url, auth, version);
          servers.add(sonarServer);
        }
      } else {
        // Defaults
        return Arrays.<ISonarServer>asList(getDefault());
      }
    } catch (final BackingStoreException e) {
      SonarCorePlugin.getDefault().error(e.getMessage(), e);
    }
    return servers;
  }

  @Override
  public void addServer(final ISonarServer server) {
    final String encodedUrl = EncodingUtils.encodeSlashes(server.getUrl());
    final IEclipsePreferences rootNode = InstanceScope.INSTANCE.getNode(SonarCorePlugin.PLUGIN_ID);
    try {
      final Preferences serversNode = rootNode.node(ISonarServerPreferenceConstansts.PREF_SERVERS);
      serversNode.put(ISonarServerPreferenceConstansts.INITIALIZED, Boolean.TRUE.toString());
      serversNode.node(encodedUrl).putBoolean(ISonarServerPreferenceConstansts.AUTH, server.hasCredentials());
      serversNode.flush();
    } catch (final BackingStoreException e) {
      SonarCorePlugin.getDefault().error(e.getMessage(), e);
    }
  }



  @Override
  public void removeServer(final ISonarServer server) {
    final String encodedUrl = EncodingUtils.encodeSlashes(server.getUrl());
    final IEclipsePreferences rootNode = InstanceScope.INSTANCE.getNode(SonarCorePlugin.PLUGIN_ID);
    try {
      final Preferences serversNode = rootNode.node(ISonarServerPreferenceConstansts.PREF_SERVERS);
      serversNode.node(encodedUrl).removeNode();
      serversNode.flush();
    } catch (final BackingStoreException e) {
      SonarCorePlugin.getDefault().error(e.getMessage(), e);
    }
    Version.remove(server);
  }

  @CheckForNull
  @Override
  public ISonarServer findServer(final String url)
  {
    final Collection<ISonarServer> servers = getServers();
    for (final ISonarServer server : servers)
    {
      final String serverUrl = server.getUrl();
      if (serverUrl.equals(url))
      {
        return server;
      }
    }
    return null;
  }

  @SuppressWarnings("nls")
  @Override
  public ISonarServer getDefault() {
    return new SonarServer("http://localhost:9000");
  }

  @Override
  public ISonarServer create(final String location, final String username, final String password) {
    return new SonarServer(location, username, password);
  }

}
