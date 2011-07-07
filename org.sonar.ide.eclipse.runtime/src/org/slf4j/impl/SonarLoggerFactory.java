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
package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import java.util.HashMap;
import java.util.Map;

public class SonarLoggerFactory implements ILoggerFactory {

  Map<String, Logger> loggerMap;

  public SonarLoggerFactory() {
    loggerMap = new HashMap<String, Logger>();
  }

  public synchronized Logger getLogger(String name) {
    Logger logger = null;
    // protect against concurrent access of loggerMap
    synchronized (this) {
      logger = (Logger) loggerMap.get(name);
      if (logger == null) {
        logger = determineLogger(name);
        loggerMap.put(name, logger);
      }
    }
    return logger;
  }

  private Logger determineLogger(String name) {
    if (name != null && name.startsWith("org.sonar")) {
      return SonarLoggerBridge.SINGLETON;
    } else {
      return NOPLogger.NOP_LOGGER;
    }
  }

}
