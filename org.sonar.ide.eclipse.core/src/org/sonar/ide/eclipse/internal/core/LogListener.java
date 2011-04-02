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
package org.sonar.ide.eclipse.internal.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Translates {@link IStatus} to slf4j.
 */
public class LogListener implements ILogListener {

  public void logging(IStatus status, String plugin) {
    Logger logger = LoggerFactory.getLogger(plugin);

    LogLevel logLevel = null;
    switch (status.getSeverity()) {
      case IStatus.ERROR:
        logLevel = ERROR;
        break;
      case IStatus.WARNING:
        logLevel = WARN;
        break;
      default:
        logLevel = INFO;
        break;
    }

    if (logLevel.isEnabled(logger)) {
      log(logLevel, logger, status);
    }
  }

  private void log(LogLevel logLevel, Logger logger, IStatus status) {
    if (status == null) {
      return;
    }

    Throwable throwable = status.getException();

    logLevel.log(logger, status.getMessage(), status.getException());

    if (throwable instanceof CoreException) {
      log(logLevel, logger, ((CoreException) throwable).getStatus());
    }

    if (status.isMultiStatus()) {
      IStatus[] children = status.getChildren();
      if (children != null) {
        for (IStatus child : children) {
          log(logLevel, logger, child);
        }
      }
    }
  }

  private interface LogLevel {
    boolean isEnabled(Logger logger);

    void log(Logger logger, String message, Throwable throwable);
  }

  private static final LogLevel ERROR = new LogLevel() {
    public boolean isEnabled(Logger logger) {
      return logger.isInfoEnabled();
    }

    public void log(Logger logger, String message, Throwable throwable) {
      logger.error(message, throwable);
    }
  };

  private static final LogLevel WARN = new LogLevel() {
    public boolean isEnabled(Logger logger) {
      return logger.isInfoEnabled();
    }

    public void log(Logger logger, String message, Throwable throwable) {
      logger.warn(message, throwable);
    }
  };

  private static final LogLevel INFO = new LogLevel() {
    public boolean isEnabled(Logger logger) {
      return logger.isInfoEnabled();
    }

    public void log(Logger logger, String message, Throwable throwable) {
      logger.info(message, throwable);
    }
  };

}
