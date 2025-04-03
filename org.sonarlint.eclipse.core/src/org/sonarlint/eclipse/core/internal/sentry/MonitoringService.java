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
package org.sonarlint.eclipse.core.internal.sentry;

import org.sonarlint.eclipse.core.internal.telemetry.SonarLintTelemetry;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarsource.sonarlint.shaded.io.sentry.Sentry;
import org.sonarsource.sonarlint.shaded.io.sentry.SentryOptions;

public class MonitoringService {
  private MonitoringService() {
    // utility class
  }

  /**
   *  We are only in a Dogfooding environment enabling Sentry.io when not specifically disabled and when the
   *  environment variable is set correctly. The system property should only be used on UTs/ITs!
   */
  public static boolean isDogfoodingEnvironment() {
    return System.getProperty("sonarlint.internal.disableDogfooding") == null
      && "1".equals(System.getenv("SONARSOURCE_DOGFOODING"));
  }

  public static void init() {
    // Logging whether monitoring via Sentry is enabled or not is done in the SonarLintBackendService as the log
    // log listener for the SonarLintLogger are not available until the UI bundle activator is started!
    if (isDogfoodingEnvironment()) {
      Sentry.init(getSentryConfiguration());
    }
  }

  public static void captureCaughtException(Throwable t) {
    Sentry.captureException(t);
  }

  // Uncaught exceptions should be tagged accordingly to find them easier in Sentry.io!
  public static void captureUncaughtException(Throwable t) {
    Sentry.setTag("caught", "false");
    Sentry.captureException(t);
    Sentry.removeTag("caught");
  }

  private static SentryOptions getSentryConfiguration() {
    var sentryOptions = new SentryOptions();

    sentryOptions.setDsn("https://975239990c2d7ec9b42958790a58adb0@o1316750.ingest.us.sentry.io/4509088252690432");
    sentryOptions.setRelease(SonarLintUtils.getPluginVersion());
    sentryOptions.setEnvironment("dogfood");
    sentryOptions.setTag("ideVersion", SonarLintTelemetry.ideVersionForTelemetry());
    sentryOptions.setTag("platform", System.getProperty("os.name"));
    sentryOptions.setTag("architecture", System.getProperty("os.arch"));
    sentryOptions.addInAppInclude("org.sonarlint.eclipse");

    return sentryOptions;
  }
}
