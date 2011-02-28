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

import java.io.File;
import java.net.URL;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.JoranConfiguratorBase;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;

public class LogbackPlugin extends Plugin {

  /**
   * Should match name in "conf/logback.xml"
   */
  private static final String LOG_DIR_PROPERTY = "log.dir";

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    configureLogback();

    LoggerFactory.getLogger(getClass()).info("Yippee - Logback configured!");
  }

  private void printErr(String message) {
    System.err.println(message);
  }

  /**
   * Godin: I'm not sure is it correct way or not, but it works.
   */
  private synchronized void configureLogback() {
    File stateDir = getStateLocation().toFile();
    if (System.getProperty(LOG_DIR_PROPERTY) == null) {
      System.setProperty(LOG_DIR_PROPERTY, stateDir.getAbsolutePath());
    }

    final URL url = getBundle().getEntry("/conf/logback.xml");
    if (url != null) {
      final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
      try {
        final JoranConfiguratorBase configurator = new JoranConfigurator();
        configurator.setContext(lc);
        lc.reset();
        configurator.doConfigure(url);
      } catch (final JoranException e) {
        e.printStackTrace();
      }
      StatusPrinter.printIfErrorsOccured(lc);
    } else {
      printErr("logback.xml not found");
    }
  }

}
