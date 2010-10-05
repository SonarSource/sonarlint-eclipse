/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
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

package org.sonar.ide.eclipse.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.slf4j.LoggerFactory;

public class SonarLogger {

  private static ILog LOG;

  public static void setLog(ILog log) {
    LOG = log;
  }

  public static void log(IStatus status) {
    LOG.log(status);
  }

  public static void log(CoreException e) {
    IStatus status = e.getStatus();
    log(status);
  }

  public static void log(Throwable t) {
    log(t.getMessage(), t);
  }

  public static void log(String msg, Throwable t) {
    log(new Status(IStatus.ERROR, ISonarConstants.PLUGIN_ID, msg, t));
  }

  public static void log(String msg) {
    LoggerFactory.getLogger(SonarLogger.class).info(msg);
    log(new Status(IStatus.OK, ISonarConstants.PLUGIN_ID, msg));
  }

}
