/*
 * Copyright (c) 2005, John J. Franey
 *
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute, and/or sell copies of  the Software, and to permit persons
 * to whom  the Software is furnished  to do so, provided  that the above
 * copyright notice(s) and this permission notice appear in all copies of
 * the  Software and  that both  the above  copyright notice(s)  and this
 * permission notice appear in supporting documentation.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR  A PARTICULAR PURPOSE AND NONINFRINGEMENT
 * OF  THIRD PARTY  RIGHTS. IN  NO EVENT  SHALL THE  COPYRIGHT  HOLDER OR
 * HOLDERS  INCLUDED IN  THIS  NOTICE BE  LIABLE  FOR ANY  CLAIM, OR  ANY
 * SPECIAL INDIRECT  OR CONSEQUENTIAL DAMAGES, OR  ANY DAMAGES WHATSOEVER
 * RESULTING FROM LOSS  OF USE, DATA OR PROFITS, WHETHER  IN AN ACTION OF
 * CONTRACT, NEGLIGENCE  OR OTHER TORTIOUS  ACTION, ARISING OUT OF  OR IN
 * CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 * Except as  contained in  this notice, the  name of a  copyright holder
 * shall not be used in advertising or otherwise to promote the sale, use
 * or other dealings in this Software without prior written authorization
 * of the copyright holder.
 *
 */
package org.slf4j.pde;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.log.ExtendedLogService;
import org.osgi.framework.Bundle;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author John J. Franey
 * @author Julien HENRY
 */
public class PDELoggerFactory implements ILoggerFactory {

  // map logger name to Logger
  Map<String, Logger> loggerMap;

  // map logger name to sink bundle
  Map<String, Bundle> bundleMap;

  public PDELoggerFactory() {
    loggerMap = new HashMap<String, Logger>();
    bundleMap = new HashMap<String, Bundle>();
  }

  private ExtendedLogService getLogService() {
    return SLF4jPlugin.getDefault().getLogServiceTracker().getService();
  }

  public Logger getLogger(String name) {
    ExtendedLogService logService = getLogService();
    if (logService != null) {
      Logger ulogger = loggerMap.get(name);
      if (ulogger == null) {
        ulogger = new PDELogger(logService.getLogger(getBundle(name), name));
        loggerMap.put(name, ulogger);
      }
      return ulogger;
    }
    throw new RuntimeException("Log service is not available");
  }

  private IExtension[] getExtensions() {
    return Platform.getExtensionRegistry()
        .getExtensionPoint("org.sonar.ide.eclipse.slf4j.pde.loggers")
        .getExtensions();
  }

  // TODO: loggerMapEntries should be sensitive to dynamic bundles.
  private List<LoggerMapEntry> loggerMapEntries;

  /*
   * get logger map entries by reading the registered extensions.
   */
  private List<LoggerMapEntry> getLoggerMapEntries() {
    if (loggerMapEntries == null) {
      List<LoggerMapEntry> entries = new ArrayList<LoggerMapEntry>();
      IExtension[] extensions = getExtensions();
      for (int i = 0; i < extensions.length; i++) {
        IConfigurationElement[] configs = extensions[i]
            .getConfigurationElements();
        for (int j = 0; j < configs.length; j++) {
          IConfigurationElement c = configs[j];
          if ("loggerMapEntry".equals(c.getName())) {
            String regex = c.getAttribute("regexp");
            String symbolicName = c.getAttribute("symbolicName");
            LoggerMapEntry e = new LoggerMapEntry();
            e.setRegex(regex);
            e.setSymbolicName(symbolicName);
            entries.add(e);
          }
        }
      }
      loggerMapEntries = entries;
    }
    return loggerMapEntries;
  }

  private Bundle getBundle(String loggerName) {

    Bundle b = bundleMap.get(loggerName);
    if (b == null) {
      // default to slf4j's ILog
      b = SLF4jPlugin.getDefault().getBundle();
      List<LoggerMapEntry> l = getLoggerMapEntries();
      for (LoggerMapEntry entry : l) {
        if (entry.matches(loggerName)) {
          b = Platform.getBundle(entry.getSymbolicName());
          bundleMap.put(loggerName, b);
          break;
        }
      }
    }
    return b;
  }
}
