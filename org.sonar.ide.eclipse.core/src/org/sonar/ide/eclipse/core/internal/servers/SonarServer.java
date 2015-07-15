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

import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.security.storage.EncodingUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.sonar.ide.eclipse.common.servers.ISonarServer;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;

@SuppressWarnings("nls")
final class SonarServer implements ISonarServer {

  private final String url;
  private final boolean auth;
  private Version version;

  SonarServer(final String url, final String username, final String password) {
    this(url, StringUtils.isNotBlank(password) && StringUtils.isNotBlank(username));
    if (auth) {
      setKeyForServerNode("username", username, false);
      setKeyForServerNode("password", password, true);
    }
  }

  SonarServer(final String url) {
    this(url, false);
  }

  SonarServer(final String url, final boolean auth) {
    Assert.isNotNull(url);
    this.url = url;
    this.auth = auth;
  }

  /**
   * @param url
   * @param auth
   * @param version
   */
  public SonarServer(final String url, final boolean auth, final Version version) {
    this(url, auth);
    this.version = version;
  }

  @Override
  public String getUrl() {
    return url;
  }

  @Override
  public boolean hasCredentials() {
    return StringUtils.isNotBlank(getPassword()) && StringUtils.isNotBlank(getUsername());
  }

  @Override
  public String getUsername() {
    return auth ? getKeyFromServerNode("username") : "";
  }

  @Override
  public String getPassword() {
    return auth ? getKeyFromServerNode("password") : "";
  }

  @CheckForNull
  @Override
  public String getVersion() {
    return version.get(this);
  }


  private String getKeyFromServerNode(final String key) {
    try {
      return SecurePreferencesFactory.getDefault().node(ISonarServerPreferenceConstansts.PREF_SERVERS).node(EncodingUtils.encodeSlashes(getUrl())).get(key, "");
    } catch (final StorageException e) {
      return "";
    }
  }

  private void setKeyForServerNode(final String key, final String value, final boolean encrypt) {
    try {
      final ISecurePreferences serverNode = SecurePreferencesFactory.getDefault().node(ISonarServerPreferenceConstansts.PREF_SERVERS)
        .node(EncodingUtils.encodeSlashes(getUrl()));
      serverNode.put(key, value, encrypt);
    } catch (final StorageException e) {
      SonarCorePlugin.getDefault().error(e.getMessage(), e);
    }
  }

  @Override
  public String toString() {
    return "SonarServer [url=" + url + ", auth=" + auth + "]";
  }

  @Override
  public int hashCode() {
    return getUrl().hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof SonarServer) {
      final SonarServer sonarServer = (SonarServer) obj;
      return getUrl().equals(sonarServer.getUrl());
    }
    return false;
  }

}
