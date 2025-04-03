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

import java.io.PrintWriter;
import java.io.StringWriter;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.statushandlers.StatusAdapter;
import org.eclipse.ui.statushandlers.StatusManager.INotificationListener;
import org.sonarlint.eclipse.core.internal.sentry.MonitoringService;

/**
 *  In case of an uncaught exception in our plug-in, one that is not logged to the SonarQube Console but rather spawns
 *  a "Problem Occurred" / "An error has occurred. See error log for more details" dialog, we want to still raise this
 *  with Sentry.io if possible.
 *
 *  This dialog is spawned by the IDE in case of something raising an IStatus with the severity "error" (IStatus.ERROR)
 *  and all the handlers are notified - the dialog is also only a handler.
 *
 *  INFO: We ourselves raise "IStatus.ERROR" a couple of times in our plug-in and in most of these cases we also log
 *  the exception to the SonarQube Console, that would trigger Sentry.io. So it might be the case that for these few
 *  cases we trigger Sentry.io twice, but this is okay!
 *  See the usage of "IStatus.ERROR" in our code base at:
 *  https://github.com/search?q=repo%3ASonarSource%2Fsonarlint-eclipse%20%22IStatus.ERROR%22&type=code
 *
 *  The message in the "Problem Occurred" dialog is always the same, but we cannot filter on that with
 *  "IStatus.getMessage()" and exclude every other dialog (handler) because this one is localized.
 */
public class UncaughtExceptionHandler implements INotificationListener {
  private static final UncaughtExceptionHandler INSTANCE = new UncaughtExceptionHandler();

  private UncaughtExceptionHandler() {
    // Singleton to not have the exceptions raised multiple times by Sentry.io in case this is somehow added more than
    // once to the StatusManager!
  }

  public static UncaughtExceptionHandler getInstance() {
    return INSTANCE;
  }

  @Override
  public void statusManagerNotified(int type, StatusAdapter[] adapters) {
    for (var adapter : adapters) {

      var status = adapter.getStatus();
      var exception = status.getException();

      if (IStatus.ERROR != status.getSeverity()
        || exception == null
        || !stacktraceContainsRelatedClasses(exception)) {
        continue;
      }

      MonitoringService.captureUncaughtException(exception);
    }
  }

  private static boolean stacktraceContainsRelatedClasses(Throwable t) {
    var sw = new StringWriter();
    var pw = new PrintWriter(sw);
    t.printStackTrace(pw);

    return sw.toString().contains("org.sonarlint.eclipse");
  }
}
