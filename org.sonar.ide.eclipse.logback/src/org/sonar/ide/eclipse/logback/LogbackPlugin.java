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
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.JoranConfiguratorBase;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;

public class LogbackPlugin extends Plugin {

  private static final String PLUGIN_ID = "org.sonar.ide.eclipse.logback"; //$NON-NLS-1$

  /**
   * Should match name in "conf/logback.xml"
   */
  private static final String LOG_DIR_PROPERTY = "log.dir"; //$NON-NLS-1$

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);

    String configFileProperty = System.getProperty(ContextInitializer.CONFIG_FILE_PROPERTY);
    if (configFileProperty != null) {
      // The standard logback config file property is set - don't force our configuration
      System.out.println(ContextInitializer.CONFIG_FILE_PROPERTY + "=" + configFileProperty); //$NON-NLS-1$
    } else {
      configureLogback();
      LogHelper.log(context, LoggerFactory.getLogger(getClass()));
    }
  }

  private synchronized void configureLogback() {
    File stateDir = getStateLocation().toFile();
    if (System.getProperty(LOG_DIR_PROPERTY) == null) {
      System.setProperty(LOG_DIR_PROPERTY, stateDir.getAbsolutePath());
    }

    try {
      final URL url = getBundle().getEntry("/conf/logback.xml"); //$NON-NLS-1$
      loadConfig(url);
    } catch (Exception e) {
      e.printStackTrace();
      getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, "Exception while configuring logging: " + e.getMessage(), e)); //$NON-NLS-1$
    }
  }

  private void loadConfig(URL url) throws JoranException {
    System.out.println("Initializing logback"); //$NON-NLS-1$
    final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    final JoranConfiguratorBase configurator = new JoranConfigurator();
    configurator.setContext(lc);
    lc.reset();
    configurator.doConfigure(url);
    StatusPrinter.printIfErrorsOccured(lc);
  }

}
