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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;

public class EclipseLogAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
  private static final String BUNDLE_ID = "org.sonar.ide.eclipse.logback.appenders"; //$NON-NLS-1$

  private Bundle bundle;

  @Override
  protected void append(ILoggingEvent logEvent) {
    int severity = 0;
    switch (logEvent.getLevel().levelInt) {
      case Level.ERROR_INT:
        severity = IStatus.ERROR;
        break;
      case Level.WARN_INT:
        severity = IStatus.WARNING;
        break;
      case Level.INFO_INT:
        severity = IStatus.INFO;
        break;
      default:
        return;
    }

    IStatus status = new Status(severity, BUNDLE_ID, logEvent.getFormattedMessage(), getThrowable(logEvent));
    ILog eclipseLog = Platform.getLog(getBundle());
    eclipseLog.log(status);
  }

  private Bundle getBundle() {
    if (bundle == null) {
      bundle = Platform.getBundle(BUNDLE_ID);
    }
    return bundle;
  }

  private Throwable getThrowable(ILoggingEvent logEvent) {
    IThrowableProxy throwableProxy = logEvent.getThrowableProxy();
    if (throwableProxy instanceof ThrowableProxy) {
      return ((ThrowableProxy) throwableProxy).getThrowable();
    }
    return null;
  }
}
