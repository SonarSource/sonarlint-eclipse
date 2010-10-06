package org.sonar.ide.eclipse.core;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.equinox.security.storage.EncodingUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;

public final class SonarServer extends PlatformObject {

  private String serverUrl;

  public SonarServer(String serverUrl) {
    Assert.isNotNull(serverUrl);
    this.serverUrl = serverUrl;
  }

  public String getServerUrl() {
    return serverUrl;
  }

  private ISecurePreferences getSecurePreferences() {
    ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault().node(ISonarConstants.PLUGIN_ID);
    securePreferences = securePreferences.node(EncodingUtils.encodeSlashes(getServerUrl()));
    return securePreferences;
  }

  public void setCredentials(String username, String password) {
    Assert.isNotNull(username);
    Assert.isNotNull(password);
    ISecurePreferences securePreferences = getSecurePreferences();
    try {
      securePreferences.put("username", username, false);
      securePreferences.put("password", password, true);
    } catch (StorageException e) {
      SonarLogger.log(e);
    }
  }

  public void flushCredentials() {
    ISecurePreferences securePreferences = getSecurePreferences();
    securePreferences.removeNode();
  }

  @Override
  public int hashCode() {
    return getServerUrl().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof SonarServer) {
      SonarServer sonarServer = (SonarServer) obj;
      return getServerUrl().equals(sonarServer.getServerUrl());
    }
    return false;
  }

  @Override
  public String toString() {
    return getServerUrl();
  }

}
