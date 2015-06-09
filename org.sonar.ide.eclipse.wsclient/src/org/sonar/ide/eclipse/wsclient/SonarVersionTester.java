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
package org.sonar.ide.eclipse.wsclient;

import org.osgi.framework.Version;

public class SonarVersionTester {

  private SonarVersionTester() {
    // Utility class
  }

  public static boolean isServerVersionSupported(String minimalVersion, String serverCurrentVersion) {
    return parseServerVersion(minimalVersion).compareTo(parseServerVersion(serverCurrentVersion)) <= 0;
  }

  private static Version parseServerVersion(String version) {
    if ("unknown".equals(version)) {
      throw new IllegalStateException("Version is unknown. Server may be not reachable.");
    }
    int i = version.indexOf('-');
    if (i != -1) {
      version = version.substring(0, i);
    }
    version += ".0"; //$NON-NLS-1$
    return Version.parseVersion(version);
  }

}
