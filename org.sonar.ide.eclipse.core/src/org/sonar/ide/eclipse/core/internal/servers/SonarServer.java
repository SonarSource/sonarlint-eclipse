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

import java.util.Properties;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.security.storage.EncodingUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.SonarProperties;
import org.sonar.ide.eclipse.wsclient.ISonarServer;
import org.sonar.runner.api.EmbeddedRunner;
import org.sonar.runner.api.IssueListener;
import org.sonar.runner.api.LogOutput;

public final class SonarServer implements ISonarServer {

  private final String id;
  private String url;
  private boolean hasCredentials;
  private String version;
  private boolean disabled;
  private EmbeddedRunner runner;

  public SonarServer(String id, String url, String username, String password) {
    this(id, url, StringUtils.isNotBlank(password) && StringUtils.isNotBlank(username));
    if (hasCredentials) {
      setKeyForServerNode("username", username, false);
      setKeyForServerNode("password", password, true);
    }
  }

  public SonarServer(String id, String url) {
    this(id, url, false);
  }

  public SonarServer(String id, String url, boolean auth) {
    Assert.isNotNull(id);
    Assert.isNotNull(url);
    this.id = id;
    this.url = url;
    this.hasCredentials = auth;
  }

  public String getId() {
    return id;
  }

  @Override
  public String getUrl() {
    return url;
  }

  @Override
  public boolean disabled() {
    return disabled;
  }

  @Override
  public boolean hasCredentials() {
    return StringUtils.isNotBlank(getPassword()) && StringUtils.isNotBlank(getUsername());
  }

  @Override
  public String getUsername() {
    return hasCredentials ? getKeyFromServerNode("username", false) : "";
  }

  @Override
  public String getPassword() {
    return hasCredentials ? getKeyFromServerNode("password", true) : "";
  }

  @CheckForNull
  public String getVersion() {
    return version;
  }

  public void setVersion(@Nullable String version) {
    this.version = version;
  }

  private String getKeyFromServerNode(String key, boolean encrypted) {
    try {
      ISecurePreferences serversNode = SecurePreferencesFactory.getDefault().node(SonarServersManager.PREF_SERVERS);
      String encodedUrl = EncodingUtils.encodeSlashes(getUrl());
      if (serversNode.nodeExists(encodedUrl)) {
        // Migration
        serversNode.node(EncodingUtils.encodeSlashes(getId())).put(key, serversNode.node(encodedUrl).get(key, ""), encrypted);
        serversNode.remove(encodedUrl);
      }
      return serversNode.node(EncodingUtils.encodeSlashes(getId())).get(key, "");
    } catch (StorageException e) {
      return "";
    }
  }

  private void setKeyForServerNode(String key, String value, boolean encrypt) {
    try {
      ISecurePreferences serverNode = SecurePreferencesFactory.getDefault().node(SonarServersManager.PREF_SERVERS)
        .node(EncodingUtils.encodeSlashes(getId()));
      serverNode.put(key, value, encrypt);
    } catch (StorageException e) {
      SonarCorePlugin.getDefault().error(e.getMessage(), e);
    }
  }

  @Override
  public String toString() {
    return "SonarServer [id=" + id + ", url=" + url + ", auth=" + hasCredentials + "]";
  }

  @Override
  public int hashCode() {
    return getUrl().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof SonarServer) {
      SonarServer sonarServer = (SonarServer) obj;
      return getUrl().equals(sonarServer.getUrl());
    }
    return false;
  }

  public void setDisabled(boolean disabled) {
    this.disabled = disabled;
  }

  public synchronized void startAnalysis(Properties props, IssueListener issueListener) {
    start();
    if (SonarCorePlugin.getDefault().isDebugEnabled()) {
      props.setProperty(SonarProperties.VERBOSE_PROPERTY, "true");
    }
    runner.runAnalysis(props, issueListener);
  }

  public synchronized void synchCaches(String projectKey) {
    start();
    runner.syncProject(projectKey);
  }

  public synchronized void start() {
    if (runner == null) {
      tryStart();
    }
  }

  private void tryStart() {
    Properties globalProps = new Properties();
    globalProps.setProperty(SonarProperties.SONAR_URL, getUrl());
    if (StringUtils.isNotBlank(getUsername())) {
      globalProps.setProperty(SonarProperties.SONAR_LOGIN, getUsername());
      globalProps.setProperty(SonarProperties.SONAR_PASSWORD, getPassword());
    }
    globalProps.setProperty(SonarProperties.ANALYSIS_MODE, SonarProperties.ANALYSIS_MODE_ISSUES);
    if (SonarCorePlugin.getDefault().isDebugEnabled()) {
      globalProps.setProperty(SonarProperties.VERBOSE_PROPERTY, "true");
    }
    globalProps.setProperty(SonarProperties.WORK_DIR, ResourcesPlugin.getWorkspace().getRoot().getLocation().append(".sonar").append(id).toString());
    runner = EmbeddedRunner.create(new LogOutput() {

      @Override
      public void log(String msg, Level level) {
        switch (level) {
          case TRACE:
            SonarCorePlugin.getDefault().debug(msg + System.lineSeparator());
            break;
          case DEBUG:
            SonarCorePlugin.getDefault().debug(msg + System.lineSeparator());
            break;
          case INFO:
            SonarCorePlugin.getDefault().info(msg + System.lineSeparator());
            break;
          case WARN:
            SonarCorePlugin.getDefault().info(msg + System.lineSeparator());
            break;
          case ERROR:
            SonarCorePlugin.getDefault().error(msg + System.lineSeparator());
            break;
          default:
            SonarCorePlugin.getDefault().info(msg + System.lineSeparator());
        }

      }
    })
      .setApp("Eclipse", SonarCorePlugin.getDefault().getBundle().getVersion().toString())
      .addGlobalProperties(globalProps);
    try {
      SonarCorePlugin.getDefault().info("Starting SonarQube for server " + id + System.lineSeparator());
      runner.start();
      this.version = runner.serverVersion();
    } catch (Throwable e) {
      SonarCorePlugin.getDefault().error("Unable to start SonarQube for server " + id + System.lineSeparator(), e);
      runner = null;
      disabled = true;
    }
  }

  public void stop() {
    if (runner != null) {
      runner.stop();
      runner = null;
    }
  }

}
