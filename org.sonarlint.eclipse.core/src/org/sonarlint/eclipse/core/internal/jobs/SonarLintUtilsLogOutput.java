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
package org.sonarlint.eclipse.core.internal.jobs;

import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarsource.sonarlint.core.client.utils.ClientLogOutput;

public final class SonarLintUtilsLogOutput implements ClientLogOutput {

  @Override
  public void log(String msg, Level level) {
    switch (level) {
      case TRACE:
      case DEBUG:
        SonarLintLogger.get().debug(msg);
        break;
      case INFO:
      case WARN:
        SonarLintLogger.get().info(msg);
        break;
      case ERROR:
        SonarLintLogger.get().error(msg);
        break;
      default:
        SonarLintLogger.get().info(msg);
    }

  }
}
