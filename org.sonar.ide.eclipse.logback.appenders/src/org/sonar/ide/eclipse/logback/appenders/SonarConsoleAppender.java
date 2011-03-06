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
package org.sonar.ide.eclipse.logback.appenders;

import org.sonar.ide.eclipse.ui.ISonarConsole;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.sonar.ide.eclipse.ui.SonarUiPlugin;

public class SonarConsoleAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
  private static final String BUNDLE_ID = "org.sonar.ide.eclipse.ui"; //$NON-NLS-1$

  private Bundle bundle;

  @Override
  protected void append(ILoggingEvent logEvent) {
    if (!isActive()) {
      return;
    }
    ISonarConsole sonarConsole = SonarUiPlugin.getDefault().getSonarConsole();
    String msg = logEvent.toString();
    switch (logEvent.getLevel().levelInt) {
      case Level.DEBUG_INT:
        sonarConsole.debug(msg);
        break;
      case Level.ERROR_INT:
        sonarConsole.error(msg);
        break;
      case Level.WARN_INT:
      case Level.INFO_INT:
      default:
        sonarConsole.info(msg);
        break;
    }
  }

  private boolean isActive() {
    if (bundle == null) {
      bundle = Platform.getBundle(BUNDLE_ID);
      if (bundle == null) {
        // TODO log couldn't find bundle
        return false;
      }
    }
    return bundle.getState() == Bundle.ACTIVE;
  }
}
