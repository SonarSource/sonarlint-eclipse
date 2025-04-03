/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
 * sonarlint@sonarsource.com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.ui.internal;

import org.eclipse.ui.IStartup;
import org.eclipse.ui.statushandlers.StatusManager;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.sentry.MonitoringService;
import org.sonarlint.eclipse.core.internal.sentry.SentryLogListener;

/**
 *  This invoked by the Eclipse IDE directly after the workbench started up, therefore before the plug-ins are even
 *  loaded. We initialize Sentry here as fast as possible to catch as many possible exceptions before the plug-in
 *  activators are invoked for the UI bundle: {@link SonarLintUiPlugin#start(org.osgi.framework.BundleContext)}
 *
 *  We also add a listener to SonarLintLogger in case something is logged with an error before the SonarQube Console is
 *  even available. This way we don't miss out on these corner cases either!
 *
 *  Additionally, we add a status listener for uncaught exceptions (that are triggering the infamous "Problem Occurred"
 *  dialog in the Eclipse IDE) to also capture the ones related to our plug-in and tag them specifically to be
 *  distinguishable and easier to act on in Sentry.io!
 */
public class WorkbenchStartupHandler implements IStartup {
  @Override
  public void earlyStartup() {
    MonitoringService.init();
    SonarLintLogger.get().addLogListener(new SentryLogListener());
    StatusManager.getManager().addListener(UncaughtExceptionHandler.getInstance());
  }
}
