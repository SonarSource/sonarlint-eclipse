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
package org.slf4j.pde;

import org.osgi.service.log.LogService;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

/**
 * @author john
 *
 */
public class PDELogger extends MarkerIgnoringBase {

  org.eclipse.equinox.log.Logger logger;

  public PDELogger(org.eclipse.equinox.log.Logger logger) {
    this.logger = logger;
  }

  /**
  * Is this logger instance enabled for the FINEST level?
  *
  * @return True if this Logger is enabled for level FINEST, false otherwise.
  */
  public boolean isTraceEnabled() {
    return logger.isLoggable(LogService.LOG_DEBUG);
  }

  /**
  * Log a message object at level FINEST.
  *
  * @param msg
  * - the message object to be logged
  */
  public void trace(String msg) {
    if (logger.isLoggable(LogService.LOG_DEBUG)) {
      logger.log(LogService.LOG_DEBUG, msg);
    }
  }

  /**
  * Log a message at level FINEST according to the specified format and
  * argument.
  *
  * <p>
  * This form avoids superfluous object creation when the logger is disabled
  * for level FINEST.
  * </p>
  *
  * @param format
  * the format string
  * @param arg
  * the argument
  */
  public void trace(String format, Object arg) {
    if (logger.isLoggable(LogService.LOG_DEBUG)) {
      FormattingTuple ft = MessageFormatter.format(format, arg);
      logger.log(LogService.LOG_DEBUG, ft.getMessage(), ft.getThrowable());
    }
  }

  /**
  * Log a message at level FINEST according to the specified format and
  * arguments.
  *
  * <p>
  * This form avoids superfluous object creation when the logger is disabled
  * for the FINEST level.
  * </p>
  *
  * @param format
  * the format string
  * @param arg1
  * the first argument
  * @param arg2
  * the second argument
  */
  public void trace(String format, Object arg1, Object arg2) {
    if (logger.isLoggable(LogService.LOG_DEBUG)) {
      FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
      logger.log(LogService.LOG_DEBUG, ft.getMessage(), ft.getThrowable());
    }
  }

  /**
  * Log a message at level FINEST according to the specified format and
  * arguments.
  *
  * <p>
  * This form avoids superfluous object creation when the logger is disabled
  * for the FINEST level.
  * </p>
  *
  * @param format
  * the format string
  * @param argArray
  * an array of arguments
  */
  public void trace(String format, Object... argArray) {
    if (logger.isLoggable(LogService.LOG_DEBUG)) {
      FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
      logger.log(LogService.LOG_DEBUG, ft.getMessage(), ft.getThrowable());
    }
  }

  /**
  * Log an exception (throwable) at level FINEST with an accompanying message.
  *
  * @param msg
  * the message accompanying the exception
  * @param t
  * the exception (throwable) to log
  */
  public void trace(String msg, Throwable t) {
    if (logger.isLoggable(LogService.LOG_DEBUG)) {
      logger.log(LogService.LOG_DEBUG, msg, t);
    }
  }

  /**
  * Is this logger instance enabled for the FINE level?
  *
  * @return True if this Logger is enabled for level FINE, false otherwise.
  */
  public boolean isDebugEnabled() {
    return logger.isLoggable(LogService.LOG_DEBUG);
  }

  /**
  * Log a message object at level FINE.
  *
  * @param msg
  * - the message object to be logged
  */
  public void debug(String msg) {
    if (logger.isLoggable(LogService.LOG_DEBUG)) {
      logger.log(LogService.LOG_DEBUG, msg);
    }
  }

  /**
  * Log a message at level FINE according to the specified format and argument.
  *
  * <p>
  * This form avoids superfluous object creation when the logger is disabled
  * for level FINE.
  * </p>
  *
  * @param format
  * the format string
  * @param arg
  * the argument
  */
  public void debug(String format, Object arg) {
    if (logger.isLoggable(LogService.LOG_DEBUG)) {
      FormattingTuple ft = MessageFormatter.format(format, arg);
      logger.log(LogService.LOG_DEBUG, ft.getMessage(), ft.getThrowable());
    }
  }

  /**
  * Log a message at level FINE according to the specified format and
  * arguments.
  *
  * <p>
  * This form avoids superfluous object creation when the logger is disabled
  * for the FINE level.
  * </p>
  *
  * @param format
  * the format string
  * @param arg1
  * the first argument
  * @param arg2
  * the second argument
  */
  public void debug(String format, Object arg1, Object arg2) {
    if (logger.isLoggable(LogService.LOG_DEBUG)) {
      FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
      logger.log(LogService.LOG_DEBUG, ft.getMessage(), ft.getThrowable());
    }
  }

  /**
  * Log a message at level FINE according to the specified format and
  * arguments.
  *
  * <p>
  * This form avoids superfluous object creation when the logger is disabled
  * for the FINE level.
  * </p>
  *
  * @param format
  * the format string
  * @param argArray
  * an array of arguments
  */
  public void debug(String format, Object... argArray) {
    if (logger.isLoggable(LogService.LOG_DEBUG)) {
      FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
      logger.log(LogService.LOG_DEBUG, ft.getMessage(), ft.getThrowable());
    }
  }

  /**
  * Log an exception (throwable) at level FINE with an accompanying message.
  *
  * @param msg
  * the message accompanying the exception
  * @param t
  * the exception (throwable) to log
  */
  public void debug(String msg, Throwable t) {
    if (logger.isLoggable(LogService.LOG_DEBUG)) {
      logger.log(LogService.LOG_DEBUG, msg, t);
    }
  }

  /**
  * Is this logger instance enabled for the INFO level?
  *
  * @return True if this Logger is enabled for the INFO level, false otherwise.
  */
  public boolean isInfoEnabled() {
    return logger.isLoggable(LogService.LOG_INFO);
  }

  /**
  * Log a message object at the INFO level.
  *
  * @param msg
  * - the message object to be logged
  */
  public void info(String msg) {
    if (logger.isLoggable(LogService.LOG_INFO)) {
      logger.log(LogService.LOG_INFO, msg);
    }
  }

  /**
  * Log a message at level INFO according to the specified format and argument.
  *
  * <p>
  * This form avoids superfluous object creation when the logger is disabled
  * for the INFO level.
  * </p>
  *
  * @param format
  * the format string
  * @param arg
  * the argument
  */
  public void info(String format, Object arg) {
    if (logger.isLoggable(LogService.LOG_INFO)) {
      FormattingTuple ft = MessageFormatter.format(format, arg);
      logger.log(LogService.LOG_INFO, ft.getMessage(), ft.getThrowable());
    }
  }

  /**
  * Log a message at the INFO level according to the specified format and
  * arguments.
  *
  * <p>
  * This form avoids superfluous object creation when the logger is disabled
  * for the INFO level.
  * </p>
  *
  * @param format
  * the format string
  * @param arg1
  * the first argument
  * @param arg2
  * the second argument
  */
  public void info(String format, Object arg1, Object arg2) {
    if (logger.isLoggable(LogService.LOG_INFO)) {
      FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
      logger.log(LogService.LOG_INFO, ft.getMessage(), ft.getThrowable());
    }
  }

  /**
  * Log a message at level INFO according to the specified format and
  * arguments.
  *
  * <p>
  * This form avoids superfluous object creation when the logger is disabled
  * for the INFO level.
  * </p>
  *
  * @param format
  * the format string
  * @param argArray
  * an array of arguments
  */
  public void info(String format, Object... argArray) {
    if (logger.isLoggable(LogService.LOG_INFO)) {
      FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
      logger.log(LogService.LOG_INFO, ft.getMessage(), ft.getThrowable());
    }
  }

  /**
  * Log an exception (throwable) at the INFO level with an accompanying
  * message.
  *
  * @param msg
  * the message accompanying the exception
  * @param t
  * the exception (throwable) to log
  */
  public void info(String msg, Throwable t) {
    if (logger.isLoggable(LogService.LOG_INFO)) {
      logger.log(LogService.LOG_INFO, msg, t);
    }
  }

  /**
  * Is this logger instance enabled for the WARNING level?
  *
  * @return True if this Logger is enabled for the WARNING level, false
  * otherwise.
  */
  public boolean isWarnEnabled() {
    return logger.isLoggable(LogService.LOG_WARNING);
  }

  /**
  * Log a message object at the WARNING level.
  *
  * @param msg
  * - the message object to be logged
  */
  public void warn(String msg) {
    if (logger.isLoggable(LogService.LOG_WARNING)) {
      logger.log(LogService.LOG_WARNING, msg);
    }
  }

  /**
  * Log a message at the WARNING level according to the specified format and
  * argument.
  *
  * <p>
  * This form avoids superfluous object creation when the logger is disabled
  * for the WARNING level.
  * </p>
  *
  * @param format
  * the format string
  * @param arg
  * the argument
  */
  public void warn(String format, Object arg) {
    if (logger.isLoggable(LogService.LOG_WARNING)) {
      FormattingTuple ft = MessageFormatter.format(format, arg);
      logger.log(LogService.LOG_WARNING, ft.getMessage(), ft.getThrowable());
    }
  }

  /**
  * Log a message at the WARNING level according to the specified format and
  * arguments.
  *
  * <p>
  * This form avoids superfluous object creation when the logger is disabled
  * for the WARNING level.
  * </p>
  *
  * @param format
  * the format string
  * @param arg1
  * the first argument
  * @param arg2
  * the second argument
  */
  public void warn(String format, Object arg1, Object arg2) {
    if (logger.isLoggable(LogService.LOG_WARNING)) {
      FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
      logger.log(LogService.LOG_WARNING, ft.getMessage(), ft.getThrowable());
    }
  }

  /**
  * Log a message at level WARNING according to the specified format and
  * arguments.
  *
  * <p>
  * This form avoids superfluous object creation when the logger is disabled
  * for the WARNING level.
  * </p>
  *
  * @param format
  * the format string
  * @param argArray
  * an array of arguments
  */
  public void warn(String format, Object... argArray) {
    if (logger.isLoggable(LogService.LOG_WARNING)) {
      FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
      logger.log(LogService.LOG_WARNING, ft.getMessage(), ft.getThrowable());
    }
  }

  /**
  * Log an exception (throwable) at the WARNING level with an accompanying
  * message.
  *
  * @param msg
  * the message accompanying the exception
  * @param t
  * the exception (throwable) to log
  */
  public void warn(String msg, Throwable t) {
    if (logger.isLoggable(LogService.LOG_WARNING)) {
      logger.log(LogService.LOG_WARNING, msg, t);
    }
  }

  /**
  * Is this logger instance enabled for level SEVERE?
  *
  * @return True if this Logger is enabled for level SEVERE, false otherwise.
  */
  public boolean isErrorEnabled() {
    return logger.isLoggable(LogService.LOG_ERROR);
  }

  /**
  * Log a message object at the SEVERE level.
  *
  * @param msg
  * - the message object to be logged
  */
  public void error(String msg) {
    if (logger.isLoggable(LogService.LOG_ERROR)) {
      logger.log(LogService.LOG_ERROR, msg);
    }
  }

  /**
  * Log a message at the SEVERE level according to the specified format and
  * argument.
  *
  * <p>
  * This form avoids superfluous object creation when the logger is disabled
  * for the SEVERE level.
  * </p>
  *
  * @param format
  * the format string
  * @param arg
  * the argument
  */
  public void error(String format, Object arg) {
    if (logger.isLoggable(LogService.LOG_ERROR)) {
      FormattingTuple ft = MessageFormatter.format(format, arg);
      logger.log(LogService.LOG_ERROR, ft.getMessage(), ft.getThrowable());
    }
  }

  /**
  * Log a message at the SEVERE level according to the specified format and
  * arguments.
  *
  * <p>
  * This form avoids superfluous object creation when the logger is disabled
  * for the SEVERE level.
  * </p>
  *
  * @param format
  * the format string
  * @param arg1
  * the first argument
  * @param arg2
  * the second argument
  */
  public void error(String format, Object arg1, Object arg2) {
    if (logger.isLoggable(LogService.LOG_ERROR)) {
      FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
      logger.log(LogService.LOG_ERROR, ft.getMessage(), ft.getThrowable());
    }
  }

  /**
  * Log a message at level SEVERE according to the specified format and
  * arguments.
  *
  * <p>
  * This form avoids superfluous object creation when the logger is disabled
  * for the SEVERE level.
  * </p>
  *
  * @param format
  * the format string
  * @param arguments
  * an array of arguments
  */
  public void error(String format, Object... arguments) {
    if (logger.isLoggable(LogService.LOG_ERROR)) {
      FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
      logger.log(LogService.LOG_ERROR, ft.getMessage(), ft.getThrowable());
    }
  }

  /**
  * Log an exception (throwable) at the SEVERE level with an accompanying
  * message.
  *
  * @param msg
  * the message accompanying the exception
  * @param t
  * the exception (throwable) to log
  */
  public void error(String msg, Throwable t) {
    if (logger.isLoggable(LogService.LOG_ERROR)) {
      logger.log(LogService.LOG_ERROR, msg, t);
    }
  }
}
