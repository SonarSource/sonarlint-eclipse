/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2011 SonarSource
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
package org.sonar.ide.eclipse.logback;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.slf4j.Logger;

public final class LogHelper {

  private LogHelper() {
  }

  public static void log(BundleContext context, Logger log) {
    log.info("Eclipse version : " + getEclipseVersion(context)); //$NON-NLS-1$
  }

  private static Version getEclipseVersion(BundleContext context) {
    for (Bundle bundle : context.getBundles()) {
      if ("org.eclipse.core.runtime".equals(bundle.getSymbolicName())) { //$NON-NLS-1$
        return bundle.getVersion();
      }
    }
    return null;
  }

}
