/*
 * Sonar Eclipse
 * Copyright (C) 2010-2012 SonarSource
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
package org.slf4j.impl;

import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;
import org.sonar.batch.EmbeddedSonarPlugin;
import org.sonar.batch.LogEntry;

public class SonarLoggerBridge extends MarkerIgnoringBase {

  static SonarLoggerBridge SINGLETON = new SonarLoggerBridge();

  private SonarLoggerBridge() {
    super.name = "Sonar";
  }

  private void log(LogEntry logEntry) {
    EmbeddedSonarPlugin.getDefault().log(logEntry);
  }

  /**
   * @return always false
   */
  public boolean isTraceEnabled() {
    return false;
  }

  public void trace(String msg) {
    // NOP
  }

  public void trace(String format, Object arg) {
    // NOP
  }

  public void trace(String format, Object arg1, Object arg2) {
    // NOP
  }

  public void trace(String format, Object[] argArray) {
    // NOP
  }

  public void trace(String msg, Throwable t) {
    // NOP
  }

  public boolean isDebugEnabled() {
    return true;
  }

  public void debug(String msg) {
    log(new LogEntry(LogEntry.DEBUG, msg));
  }

  public void debug(String msg, Throwable t) {
    log(new LogEntry(LogEntry.DEBUG, msg, t));
  }

  public void debug(String format, Object arg) {
    debug(MessageFormatter.format(format, arg));
  }

  public void debug(String format, Object arg1, Object arg2) {
    debug(MessageFormatter.format(format, arg1, arg2));
  }

  public void debug(String format, Object[] argArray) {
    debug(MessageFormatter.arrayFormat(format, argArray));
  }

  /**
   * @return always true
   */
  public boolean isInfoEnabled() {
    return true;
  }

  public void info(String msg) {
    log(new LogEntry(LogEntry.INFO, msg));
  }

  public void info(String msg, Throwable t) {
    log(new LogEntry(LogEntry.INFO, msg, t));
  }

  public void info(String format, Object arg) {
    info(MessageFormatter.format(format, arg));
  }

  public void info(String format, Object arg1, Object arg2) {
    info(MessageFormatter.format(format, arg1, arg2));
  }

  public void info(String format, Object[] argArray) {
    info(MessageFormatter.arrayFormat(format, argArray));
  }

  /**
   * @return always true
   */
  public boolean isWarnEnabled() {
    return true;
  }

  public void warn(String msg) {
    log(new LogEntry(LogEntry.WARNING, msg));
  }

  public void warn(String msg, Throwable t) {
    log(new LogEntry(LogEntry.WARNING, msg, t));
  }

  public void warn(String format, Object arg) {
    warn(MessageFormatter.format(format, arg));
  }

  public void warn(String format, Object arg1, Object arg2) {
    warn(MessageFormatter.format(format, arg1, arg2));
  }

  public void warn(String format, Object[] argArray) {
    warn(MessageFormatter.arrayFormat(format, argArray));
  }

  /**
   * @return always true
   */
  public boolean isErrorEnabled() {
    return true;
  }

  public void error(String msg) {
    log(new LogEntry(LogEntry.ERROR, msg));
  }

  public void error(String msg, Throwable t) {
    log(new LogEntry(LogEntry.ERROR, msg, t));
  }

  public void error(String format, Object arg) {
    error(MessageFormatter.format(format, arg));
  }

  public void error(String format, Object arg1, Object arg2) {
    error(MessageFormatter.format(format, arg1, arg2));
  }

  public void error(String format, Object[] argArray) {
    error(MessageFormatter.arrayFormat(format, argArray));
  }

}
