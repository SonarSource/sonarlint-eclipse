/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
 * sonarqube@googlegroups.com
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
import java.util.Collection;
import java.util.List;
import javax.annotation.CheckForNull;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.security.storage.EncodingUtils;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;

public class SonarServersManager implements ISonarServersManager {

  private static final String MINIMAL_VERSION = "4.2";

  private final static String[] UNSUPPORTED_VERSION_PREFIX = {"1.", "2.", "3.", "4.0", "4.1"};

  private static final String INITIALIZED_ATTRIBUTE = "initialized";

  private static final String DEFAULT_ATTRIBUTE = "default_server";

  private static final String AUTH_ATTRIBUTE = "auth";

  private static final String URL_ATTRIBUTE = "url";

  static final String PREF_SERVERS = "servers";

  private SonarServer defaultServer;
  private List<SonarServer> servers = Lists.newArrayList();

  private boolean loadedOnce = false;

  @Override
  public synchronized Collection<SonarServer> reloadServers() {
    reloadFromEclipsePreferencesAndCheckStatus();
    return servers;
  }

  @Override
  public synchronized Collection<SonarServer> getServers() {
    if (!loadedOnce) {
      reloadFromEclipsePreferencesAndCheckStatus();
    }
    return servers;
  }

  private void reloadFromEclipsePreferencesAndCheckStatus() {
    final Job job = new Job("Reload SonarQube servers status") {
      protected IStatus run(IProgressMonitor monitor) {
        stopAllServers();
        servers.clear();
        defaultServer = null;
        IEclipsePreferences rootNode = InstanceScope.INSTANCE.getNode(SonarCorePlugin.PLUGIN_ID);
        try {
          rootNode.sync();
          Preferences serversNode = rootNode.nodeExists(PREF_SERVERS) ? rootNode.node(PREF_SERVERS) : DefaultScope.INSTANCE.getNode(SonarCorePlugin.PLUGIN_ID).node(PREF_SERVERS);
          String defaultId = serversNode.get(DEFAULT_ATTRIBUTE, "");
          for (String idOrEncodedUrl : serversNode.childrenNames()) {
            Preferences serverNode = serversNode.node(idOrEncodedUrl);
            String id;
            String url = serverNode.get(URL_ATTRIBUTE, null);
            if (url != null) {
              id = EncodingUtils.decodeSlashes(idOrEncodedUrl);
            } else {
              url = EncodingUtils.decodeSlashes(idOrEncodedUrl);
              id = url;
            }
            boolean auth = serverNode.getBoolean(AUTH_ATTRIBUTE, false);
            SonarServer sonarServer = new SonarServer(id, url, auth);
            if (defaultId.equals(sonarServer.getId())) {
              defaultServer = sonarServer;
            }
            sonarServer.start();
            String serverVersion = sonarServer.getVersion();
            boolean started = true;
            if (serverVersion != null) {
              for (String prefix : UNSUPPORTED_VERSION_PREFIX) {
                if (serverVersion.startsWith(prefix)) {
                  SonarCorePlugin.getDefault()
                    .error("SonarQube server " + serverVersion + " at " + url + " is not supported. Minimal supported version is " + MINIMAL_VERSION + "\n");
                  started = false;
                  break;
                }
              }
            } else {
              started = false;
            }
            sonarServer.setStarted(started);
            servers.add(sonarServer);
          }
          if (defaultServer == null && !servers.isEmpty()) {
            defaultServer = servers.get(0);
          }
          if (defaultServer != null && !defaultId.equals(defaultServer.getId())) {
            serversNode.put(DEFAULT_ATTRIBUTE, defaultServer.getId());
            serversNode.flush();
          }
        } catch (BackingStoreException e) {
          SonarCorePlugin.getDefault().error(e.getMessage(), e);
        }
        loadedOnce = true;
        return Status.OK_STATUS;
      }
    };
    job.setPriority(Job.LONG);
    job.schedule();
    try {
      job.join();
    } catch (InterruptedException e) {
      // Ignore
    }
  }

  @Override
  public synchronized void addServer(SonarServer server) {
    IEclipsePreferences rootNode = InstanceScope.INSTANCE.getNode(SonarCorePlugin.PLUGIN_ID);
    try {
      Preferences serversNode = rootNode.node(PREF_SERVERS);
      serversNode.put(INITIALIZED_ATTRIBUTE, "true");
      if (defaultServer == null) {
        defaultServer = server;
        serversNode.put(DEFAULT_ATTRIBUTE, server.getId());
      }
      Preferences serverNode = serversNode.node(EncodingUtils.encodeSlashes(server.getId()));
      serverNode.put(URL_ATTRIBUTE, server.getUrl());
      serverNode.putBoolean(AUTH_ATTRIBUTE, server.hasCredentials());
      serversNode.flush();
    } catch (BackingStoreException e) {
      SonarCorePlugin.getDefault().error(e.getMessage(), e);
    }
    reloadFromEclipsePreferencesAndCheckStatus();
  }

  /**
   * For tests.
   */
  public synchronized void clean() {
    stopAllServers();
    servers.clear();
    defaultServer = null;
    IEclipsePreferences rootNode = InstanceScope.INSTANCE.getNode(SonarCorePlugin.PLUGIN_ID);
    try {
      rootNode.node(PREF_SERVERS).removeNode();
      rootNode.node(PREF_SERVERS).put(INITIALIZED_ATTRIBUTE, "true");
      rootNode.flush();
    } catch (BackingStoreException e) {
      SonarCorePlugin.getDefault().error(e.getMessage(), e);
    }
  }

  public void stopAllServers() {
    for (SonarServer server : servers) {
      server.stop();
    }
  }

  @Override
  public synchronized void removeServer(SonarServer server) {
    IEclipsePreferences rootNode = InstanceScope.INSTANCE.getNode(SonarCorePlugin.PLUGIN_ID);
    try {
      Preferences serversNode = rootNode.node(PREF_SERVERS);
      serversNode.put(INITIALIZED_ATTRIBUTE, "true");
      serversNode.node(EncodingUtils.encodeSlashes(server.getId())).removeNode();
      serversNode.flush();
    } catch (BackingStoreException e) {
      SonarCorePlugin.getDefault().error(e.getMessage(), e);
    }
    reloadFromEclipsePreferencesAndCheckStatus();
  }

  @CheckForNull
  @Override
  public synchronized SonarServer findServer(String idOrUrl) {
    for (SonarServer server : getServers()) {
      if (server.getId().equals(idOrUrl) || server.getUrl().equals(idOrUrl)) {
        return server;
      }
    }
    return null;
  }

  @Override
  public SonarServer create(String id, String location, String username, String password) {
    return new SonarServer(id, location, username, password);
  }

  public SonarServer getDefaultServer() {
    return defaultServer;
  }

  public void setDefault(SonarServer server) {
    IEclipsePreferences rootNode = InstanceScope.INSTANCE.getNode(SonarCorePlugin.PLUGIN_ID);
    try {
      Preferences serversNode = rootNode.node(PREF_SERVERS);
      serversNode.put(DEFAULT_ATTRIBUTE, server.getId());
      serversNode.flush();
    } catch (BackingStoreException e) {
      SonarCorePlugin.getDefault().error(e.getMessage(), e);
    }
    reloadFromEclipsePreferencesAndCheckStatus();
  }

}
