/*
 * Copyright (C) 2010 Evgeny Mandrikov
 *
 * Sonar-IDE is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar-IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar-IDE; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.console.SonarConsole;
import org.sonar.ide.eclipse.internal.project.SonarProjectManager;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.JoranConfiguratorBase;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * @author Jérémie Lagarde
 */
public class SonarPlugin extends AbstractUIPlugin {

  // The plug-in ID
  public static final String PLUGIN_ID = "org.sonar.ide.eclipse";

  // Markers
  public static final String MARKER_VIOLATION_ID = PLUGIN_ID + ".sonarViolationMarker"; //$NON-NLS-1$
  public static final String MARKER_DUPLICATION_ID = PLUGIN_ID + ".sonarDuplicationMarker"; //$NON-NLS-1$
  public static final String MARKER_COVERAGE_ID = PLUGIN_ID + ".sonarCoverageMarker"; //$NON-NLS-1$

  // Images
  private static ImageDescriptor SONARWIZBAN_IMG;
  private static ImageDescriptor SONAR16_IMG;
  private static ImageDescriptor SONAR32_IMG;
  private static ImageDescriptor SONARCONSOLE_IMG;
  private static ImageDescriptor SONARSYNCHRO_IMG;
  private static ImageDescriptor SONARCLOSE_IMG;

  public static final String IMG_SONARWIZBAN = "sonar_wizban.gif"; //$NON-NLS-1$
  public static final String IMG_SONAR16 = "sonar.png"; //$NON-NLS-1$
  public static final String IMG_SONAR32 = "sonar32.png"; //$NON-NLS-1$
  public static final String IMG_SONARCONSOLE = "sonar.png"; //$NON-NLS-1$
  public static final String IMG_SONARSYNCHRO = "synced.gif"; //$NON-NLS-1$
  public static final String IMG_SONARCLOSE = "close.gif"; //$NON-NLS-1$

  // The shared instance
  private static SonarPlugin plugin;

  private static SonarServerManager serverManager;
  private static SonarProjectManager projectManager;

  private SonarConsole console;

  public SonarPlugin() {
  }

  public static SonarServerManager getServerManager() {
    if (serverManager == null) {
      serverManager = new SonarServerManager();
    }
    return serverManager;
  }

  public SonarProjectManager getProjectManager() {
    if (projectManager == null) {
      projectManager = new SonarProjectManager();
    }
    return projectManager;
  }

  @Override
  public void start(final BundleContext context) throws Exception {
    super.start(context);
    plugin = this;

    initLogging();
    try {
      console = new SonarConsole();
    } catch (final RuntimeException e) {
      writeLog(IStatus.ERROR, "Errors occurred starting the Sonar console", e); //$NON-NLS-1$
    }

    LoggerFactory.getLogger(SonarPlugin.class).info("Sonar plugin started");
  }

  /**
   * Godin: I'm not sure is it correct way or not, but it works.
   */
  private void initLogging() {
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
      System.err.println("logback.xml not found");
    }
  }

  @Override
  public void stop(final BundleContext context) throws Exception {
    plugin = null;
    super.stop(context);
    if (console != null) {
      console.shutdown();
    }

    LoggerFactory.getLogger(SonarPlugin.class).info("Sonar plugin stopped");
  }

  /**
   * @return the shared instance
   */
  public static SonarPlugin getDefault() {
    return plugin;
  }

  public SonarConsole getConsole() {
    return this.console;
  }

  private IStatus createStatus(final int severity, final String msg, final Throwable t) {
    return new Status(severity, PLUGIN_ID, msg, t);
  }

  public void writeLog(final int severity, final String msg, final Throwable t) {
    super.getLog().log(createStatus(severity, msg, t));
  }

  public void writeLog(final IStatus status) {
    super.getLog().log(status);
  }

  public void displayMessage(final int severity, final String msg) {
    final Display display = PlatformUI.getWorkbench().getDisplay();
    display.syncExec(new Runnable() {

      public void run() {
        switch (severity) {
          case IStatus.ERROR:
            MessageDialog.openError(display.getActiveShell(), Messages.getString("error"), msg); //$NON-NLS-1$
            break;
          case IStatus.WARNING:
            MessageDialog.openWarning(display.getActiveShell(), Messages.getString("warning"), msg); //$NON-NLS-1$
            break;
        }
      }
    });
  }

  public void displayError(final int severity, final String msg, final Throwable t, final boolean shouldLog) {
    final IStatus status = createStatus(severity, msg, t);
    if (shouldLog) {
      writeLog(status);
    }
    final Display display = PlatformUI.getWorkbench().getDisplay();
    display.syncExec(new Runnable() {

      public void run() {
        ErrorDialog.openError(display.getActiveShell(), null, Messages.getString("error"), status); //$NON-NLS-1$
      }
    });
  }

  public static ImageDescriptor getImageDescriptor(final String id) {
    ImageDescriptor img = getCachedImageDescriptor(id);
    if (img == null) {
      img = loadImageDescriptor(id);
    }
    return img;
  }

  private static ImageDescriptor loadImageDescriptor(final String id) {
    final String iconPath = "icons/"; //$NON-NLS-1$

    try {
      final URL installURL = SonarPlugin.getDefault().getBundle().getEntry("/"); //$NON-NLS-1$
      final URL url = new URL(installURL, iconPath + id);
      return ImageDescriptor.createFromURL(url);
    } catch (final MalformedURLException e) {
      return ImageDescriptor.getMissingImageDescriptor();
    }
  }

  private static ImageDescriptor getCachedImageDescriptor(final String id) {
    ImageDescriptor img = null;
    if (id.equals(IMG_SONARWIZBAN)) {
      if (SONARWIZBAN_IMG == null) {
        SONARWIZBAN_IMG = loadImageDescriptor(IMG_SONARWIZBAN);
      }
      img = SONARWIZBAN_IMG;
    }
    if (id.equals(IMG_SONAR16)) {
      if (SONAR16_IMG == null) {
        SONAR16_IMG = loadImageDescriptor(IMG_SONAR16);
      }
      img = SONARCONSOLE_IMG;
    }
    if (id.equals(IMG_SONAR32)) {
      if (SONAR32_IMG == null) {
        SONAR32_IMG = loadImageDescriptor(IMG_SONAR32);
      }
      img = SONARCONSOLE_IMG;
    }
    if (id.equals(IMG_SONARCONSOLE)) {
      if (SONARCONSOLE_IMG == null) {
        SONARCONSOLE_IMG = loadImageDescriptor(IMG_SONARCONSOLE);
      }
      img = SONARCONSOLE_IMG;
    }
    if (id.equals(IMG_SONARSYNCHRO)) {
      if (SONARSYNCHRO_IMG == null) {
        SONARSYNCHRO_IMG = loadImageDescriptor(IMG_SONARSYNCHRO);
      }
      img = SONARSYNCHRO_IMG;
    }
    if (id.equals(IMG_SONARCLOSE)) {
      if (SONARCLOSE_IMG == null) {
        SONARCLOSE_IMG = loadImageDescriptor(IMG_SONARCLOSE);
      }
      img = SONARCLOSE_IMG;
    }
    return img;
  }

}
