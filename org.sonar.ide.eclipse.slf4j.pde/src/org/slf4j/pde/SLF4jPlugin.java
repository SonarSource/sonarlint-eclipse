/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2013 SonarSource
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

import org.eclipse.core.runtime.Plugin;
import org.eclipse.equinox.log.ExtendedLogService;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 *
 * @author Julien HENRY
 *
 */
public class SLF4jPlugin extends Plugin {

  // The shared instance.
  private static SLF4jPlugin plugin;
  private ServiceTracker<ExtendedLogService, ExtendedLogService> logServiceTracker;

  /**
   * The constructor.
   */
  public SLF4jPlugin() {
    plugin = this;
  }

  /**
   * This method is called upon plug-in activation
   */
  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    // create a tracker and track the log service
    logServiceTracker = new ServiceTracker<ExtendedLogService, ExtendedLogService>(context, context.getServiceReference(ExtendedLogService.class), null);
    logServiceTracker.open();
  }

  /**
   * This method is called when the plug-in is stopped
   */
  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);
    // close the service tracker
    logServiceTracker.close();
    logServiceTracker = null;
    plugin = null;
  }

  /**
   * Returns the shared instance.
   */
  public static SLF4jPlugin getDefault() {
    return plugin;
  }

  public ServiceTracker<ExtendedLogService, ExtendedLogService> getLogServiceTracker() {
    return logServiceTracker;
  }

}
