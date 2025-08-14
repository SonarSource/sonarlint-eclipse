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
package org.sonarlint.eclipse.core;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.internal.LogListener;

public class SonarLintLogger {
  private static final SonarLintLogger instance = new SonarLintLogger();
  private final List<LogListener> logListeners = new ArrayList<>();

  private SonarLintLogger() {
    // singleton
  }

  public static SonarLintLogger get() {
    return instance;
  }

  public void addLogListener(LogListener listener) {
    logListeners.add(listener);
  }

  public void removeLogListener(LogListener listener) {
    logListeners.remove(listener);
  }

  public void error(@Nullable String msg) {
    for (LogListener listener : logListeners) {
      listener.error(msg);
    }
  }

  public void error(String msg, Throwable t) {
    for (var listener : logListeners) {
      listener.error(msg, t);
    }
  }

  public void info(@Nullable String msg) {
    for (LogListener listener : logListeners) {
      listener.info(msg);
    }
  }

  public void debug(@Nullable String msg) {
    for (LogListener listener : logListeners) {
      listener.debug(msg);
    }
  }


  public void debug(String msg, Throwable t) {
    for (var listener : logListeners) {
      listener.debug(msg, t);
    }
  }

  public void traceIdeMessage(String msg) {
    for (LogListener listener : logListeners) {
      listener.traceIdeMessage(msg);
    }
  }

  public void traceIdeMessage(String msg, Throwable t) {
    for (var listener : logListeners) {
      listener.traceIdeMessage(msg, t);
    }
  }
}
