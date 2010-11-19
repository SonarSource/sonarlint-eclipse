package org.sonar.ide.eclipse.core;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.security.storage.EncodingUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.sonar.ide.eclipse.internal.core.ServersManager;
import org.sonar.wsclient.Host;

public final class SonarServer {

  private final String url;
  private final boolean auth;

  public SonarServer(String url, String username, String password) {
    this(url, StringUtils.isNotBlank(password) && StringUtils.isNotBlank(username));
    if (auth) {
      setKeyForServerNode("username", username, false);
      setKeyForServerNode("password", password, true);
    }
  }

  public SonarServer(String url) {
    this(url, false);
  }

  public SonarServer(String url, boolean auth) {
    Assert.isNotNull(url);
    this.url = url;
    this.auth = auth;
  }

  public String getUrl() {
    return url;
  }

  public boolean hasCredentials() {
    return StringUtils.isNotBlank(getPassword()) && StringUtils.isNotBlank(getUsername());
  }

  public String getUsername() {
    return auth ? getKeyFromServerNode("username") : "";
  }

  public String getPassword() {
    return auth ? getKeyFromServerNode("password") : "";
  }

  private String getKeyFromServerNode(String key) {
    try {
      return SecurePreferencesFactory.getDefault().node(ServersManager.NODE).node(EncodingUtils.encodeSlashes(getUrl())).get(key, "");
    } catch (StorageException e) {
      return "";
    }
  }

  private void setKeyForServerNode(String key, String value, boolean encrypt) {
    try {
      ISecurePreferences serverNode = SecurePreferencesFactory.getDefault().node(ServersManager.NODE)
          .node(EncodingUtils.encodeSlashes(getUrl()));
      serverNode.put(key, value, encrypt);
    } catch (StorageException e) {
      // TODO handle
    }
  }

  /**
   * For sonar-ws-client
   */
  public Host getHost() {
    return new Host(getUrl(), getUsername(), getPassword());
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

}
