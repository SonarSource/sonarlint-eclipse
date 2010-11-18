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

package org.sonar.ide.eclipse.ui;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.EclipseProxyAuthenticator;
import org.sonar.ide.eclipse.EclipseProxySelector;
import org.sonar.ide.eclipse.core.FavouriteMetricsManager;
import org.sonar.ide.eclipse.core.ISonarConstants;
import org.sonar.ide.eclipse.core.SonarLogger;
import org.sonar.ide.eclipse.internal.SonarServerManager;
import org.sonar.ide.eclipse.internal.project.SonarProjectManager;
import org.sonar.ide.eclipse.internal.ui.Messages;
import org.sonar.ide.eclipse.internal.ui.SonarUiPreferenceInitializer;
import org.sonar.ide.eclipse.jobs.RefreshViolationsJob;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.JoranConfiguratorBase;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

import java.net.Authenticator;
import java.net.ProxySelector;
import java.net.URL;

public class SonarUiPlugin extends AbstractUIPlugin {

  // The shared instance
  private static SonarUiPlugin plugin;

  private static SonarServerManager serverManager;
  private static SonarProjectManager projectManager;

  private FavouriteMetricsManager favouriteMetricsManager = new FavouriteMetricsManager();

  public SonarUiPlugin() {
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

  public static FavouriteMetricsManager getFavouriteMetricsManager() {
    return getDefault().favouriteMetricsManager;
  }

  @Override
  public void start(final BundleContext context) throws Exception {
    super.start(context);
    plugin = this;

    SonarLogger.setLog(getLog());

    setupLogging();
    setupProxy(context);
    RefreshViolationsJob.setupViolationsUpdater();

    getFavouriteMetricsManager().set(SonarUiPreferenceInitializer.getFavouriteMetrics());

    LoggerFactory.getLogger(SonarUiPlugin.class).info("SonarPlugin started");
  }

  @Override
  public void stop(final BundleContext context) throws Exception {
    SonarUiPreferenceInitializer.setFavouriteMetrics(getFavouriteMetricsManager().get());

    plugin = null;
    LoggerFactory.getLogger(SonarUiPlugin.class).info("SonarPlugin stopped");
    super.stop(context);
  }

  /**
   * @return the shared instance
   */
  public static SonarUiPlugin getDefault() {
    return plugin;
  }

  /**
   * Godin: I'm not sure is it correct way or not, but it works.
   */
  private void setupLogging() {
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
      SonarLogger.log("logback.xml not found");
    }
  }

  private void setupProxy(final BundleContext context) {
    ServiceReference proxyServiceReference = context.getServiceReference(IProxyService.class.getName());
    if (proxyServiceReference != null) {
      IProxyService proxyService = (IProxyService) context.getService(proxyServiceReference);
      ProxySelector.setDefault(new EclipseProxySelector(proxyService));
      Authenticator.setDefault(new EclipseProxyAuthenticator(proxyService));
    }
  }

  public void displayError(final int severity, final String msg, final Throwable t, final boolean shouldLog) {
    final IStatus status = new Status(severity, ISonarConstants.PLUGIN_ID, msg, t);
    if (shouldLog) {
      SonarLogger.log(status);
    }
    final Display display = PlatformUI.getWorkbench().getDisplay();
    display.syncExec(new Runnable() {
      public void run() {
        ErrorDialog.openError(display.getActiveShell(), null, Messages.getString("error"), status); //$NON-NLS-1$
      }
    });
  }

  public static boolean hasSonarNature(IProject project) {
    try {
      return project.hasNature(ISonarConstants.NATURE_ID);
    } catch (CoreException e) {
      SonarLogger.log(e);
      return false;
    }
  }

  public static boolean hasJavaNature(IProject project) {
    try {
      return project.hasNature("org.eclipse.jdt.core.javanature");
    } catch (CoreException e) {
      SonarLogger.log(e);
      return false;
    }
  }

}
