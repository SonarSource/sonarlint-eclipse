package org.sonar.ide.eclipse.tests.common;

import org.eclipse.core.resources.ResourcesPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * @author Evgeny Mandrikov
 */
public final class VersionHelpers {
  public static Version getEclipseVersion() {
    Bundle bundle = ResourcesPlugin.getPlugin().getBundle();
    String version = (String) bundle.getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION);
    return Version.parseVersion(version);
  }
  
  private VersionHelpers() {
  }
}
