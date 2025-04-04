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

import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.internal.LogListener;

/**
 *  To be independent from the SonarLintLogger and the SonarQube Console and the listener registered for it. Useful
 *  when there is SonarQube Console (yet) available and its listener was not yet registered.
 */
public class SentryLogListener implements LogListener {
  @Override
  public void info(@Nullable String msg, boolean fromAnalyzer) {
    // Irrelevant to Sentry.io for now!
  }

  @Override
  public void error(@Nullable String msg, boolean fromAnalyzer) {
    // Irrelevant to Sentry.io for now!
  }

  @Override
  public void error(@Nullable String msg, Throwable t, boolean fromAnalyzer) {
    MonitoringService.captureCaughtException(t);
  }

  @Override
  public void debug(@Nullable String msg, boolean fromAnalyzer) {
    // Irrelevant to Sentry.io for now!
  }

  @Override
  public void debug(@Nullable String msg, Throwable t, boolean fromAnalyzer) {
    // Irrelevant to Sentry.io for now!
  }

  @Override
  public void traceIdeMessage(@Nullable String msg) {
    // Irrelevant to Sentry.io for now!
  }

  @Override
  public void traceIdeMessage(@Nullable String msg, Throwable t) {
    MonitoringService.captureCaughtException(t);
  }
}
