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

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.console.SonarConsole;
import org.sonar.ide.eclipse.core.ISonarConstants;
import org.sonar.ide.eclipse.core.ISonarResource;
import org.sonar.ide.eclipse.core.SonarLogger;
import org.sonar.ide.eclipse.internal.core.SonarResource;
import org.sonar.ide.eclipse.internal.project.SonarProjectManager;
import org.sonar.ide.eclipse.jobs.RefreshViolationsJob;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.JoranConfiguratorBase;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

import java.net.Authenticator;
import java.net.ProxySelector;
import java.net.URL;

public class SonarPlugin extends AbstractUIPlugin {

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

    SonarLogger.setLog(getLog());

    setupLogging();
    setupConsole();
    setupProxy(context);
    RefreshViolationsJob.setupViolationsUpdater();

    LoggerFactory.getLogger(SonarPlugin.class).info("SonarPlugin started");
  }

  @Override
  public void stop(final BundleContext context) throws Exception {
    if (console != null) {
      console.shutdown();
    }
    plugin = null;
    LoggerFactory.getLogger(SonarPlugin.class).info("SonarPlugin stopped");
    super.stop(context);
  }

  /**
   * @return the shared instance
   */
  public static SonarPlugin getDefault() {
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

  private void setupConsole() {
    try {
      console = new SonarConsole();
    } catch (final RuntimeException e) {
      SonarLogger.log("Error occurred during Sonar console startup", e);
    }
  }

  public SonarConsole getConsole() {
    return this.console;
  }

  private IStatus createStatus(final int severity, final String msg, final Throwable t) {
    return new Status(severity, ISonarConstants.PLUGIN_ID, msg, t);
  }

  public void displayError(final int severity, final String msg, final Throwable t, final boolean shouldLog) {
    final IStatus status = createStatus(severity, msg, t);
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

  public static ISonarResource createSonarResource(IResource resource, String key) {
    return new SonarResource(resource, key);
  }

}
