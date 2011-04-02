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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.JoranConfiguratorBase;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class LogbackPlugin extends Plugin {

  private static final String PLUGIN_ID = "org.sonar.ide.eclipse.logback"; //$NON-NLS-1$

  private static final String RESOURCES_PLUGIN_ID = "org.eclipse.core.resources"; //$NON-NLS-1$

  /**
   * Should match name in "conf/logback.xml".
   */
  private static final String LOG_DIR_PROPERTY = "log.dir"; //$NON-NLS-1$

  /**
   * Indicates that our configuration was applied.
   */
  private boolean configured = false;

  private Timer timer = new Timer(PLUGIN_ID);

  private TimerTask timerTask = new TimerTask() {
    @Override
    public void run() {
      if (isPlatformInstanceLocationSet()) {
        timer.cancel();
        configureLogback();
      }
    }
  };

  @Override
  public void start(BundleContext context) throws Exception { // NOSONAR
    super.start(context);

    String configFileProperty = System.getProperty(ContextInitializer.CONFIG_FILE_PROPERTY);
    if (configFileProperty != null) {
      // The standard logback config file property is set - don't force our configuration
      systemOut(ContextInitializer.CONFIG_FILE_PROPERTY + "=" + configFileProperty); //$NON-NLS-1$
    } else {
      // log file would be created in state area of this plug-in, so we must ensure that it exists,
      // otherwise we will break process of Eclipse start-up and as a result - selection of workspace will not work
      if (isPlatformInstanceLocationSet()) {
        configureLogback();
      } else {
        systemOut("Platform instance location is not set yet - will retry."); //$NON-NLS-1$
        timer.schedule(timerTask, 0, 50);
      }
    }
  }

  private boolean isPlatformInstanceLocationSet() {
    if (!Platform.isRunning()) {
      return false;
    }
    Bundle resourcesBundle = Platform.getBundle(RESOURCES_PLUGIN_ID);
    if (resourcesBundle == null || resourcesBundle.getState() != Bundle.ACTIVE) {
      return false;
    }
    return Platform.getInstanceLocation().isSet();
  }

  private synchronized void configureLogback() {
    if (configured) {
      systemOut("Logback was configured already"); //$NON-NLS-1$
      return;
    }
    systemOut("Configuring logback"); //$NON-NLS-1$

    File stateDir = getStateLocation().toFile();
    if (System.getProperty(LOG_DIR_PROPERTY) == null) {
      System.setProperty(LOG_DIR_PROPERTY, stateDir.getAbsolutePath());
    }

    try {
      final URL url = getBundle().getEntry("/conf/logback.xml"); //$NON-NLS-1$
      loadConfig(url);
      LogHelper.log(getBundle().getBundleContext(), LoggerFactory.getLogger(getClass()));
      configured = true;
    } catch (Exception e) {
      logException(e);
    }
  }

  private void loadConfig(URL url) throws JoranException {
    ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
    if (!(loggerFactory instanceof LoggerContext)) {
      systemErr("SLF4J logger factory is not an instance of LoggerContext: " + loggerFactory.getClass().getName()); //$NON-NLS-1$
      return;
    }

    systemOut("Initializing logback"); //$NON-NLS-1$
    final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    lc.reset();

    final JoranConfiguratorBase configurator = new JoranConfigurator();
    configurator.setContext(lc);
    configurator.doConfigure(url);

    StatusPrinter.printIfErrorsOccured(lc);
  }

  private static void systemOut(String message) {
    System.out.println(PLUGIN_ID + ": " + message); //$NON-NLS-1$ // NOSONAR
  }

  private static void systemErr(String message) {
    System.err.println(PLUGIN_ID + ": " + message); //$NON-NLS-1$ // NOSONAR
  }

  private void logException(Exception e) {
    e.printStackTrace(); // NOSONAR
    getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, "Exception while configuring logging: " + e.getMessage(), e)); //$NON-NLS-1$
  }
}
