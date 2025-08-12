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
package org.sonarlint.eclipse.core.internal;

import org.eclipse.jdt.annotation.Nullable;

public interface LogListener {

  void info(@Nullable String msg);

  void error(@Nullable String msg);

  void error(@Nullable String msg, Throwable t);

  void debug(@Nullable String msg);

  void debug(@Nullable String msg, Throwable t);

  /**
   *  This should only be used for IDE-specific logging and is not intended for tracing messages from SLCORE as these
   *  ones are handled like debug messages. IDE-specific logging is something like adaptations, interaction with an
   *  extension point, ...
   */
  void traceIdeMessage(@Nullable String msg);

  void traceIdeMessage(@Nullable String msg, Throwable t);
}
