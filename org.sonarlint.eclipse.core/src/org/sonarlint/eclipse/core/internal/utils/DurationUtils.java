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
package org.sonarlint.eclipse.core.internal.utils;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.SonarLintLogger;

public class DurationUtils {
  private DurationUtils() {
    // utility class
  }

  /**
   *  Users can provide properties containing simple timeouts ("3" is "3 minutes") or more complex ones ("PT4H" is
   *  "4 hours"). We have to check all the different cases for users who configure the timeouts differently.
   */
  @Nullable
  public static Duration getTimeoutProperty(String propertyName) {
    var property = System.getProperty(propertyName);
    if (property == null) {
      return null;
    }

    try {
      return Duration.ofMinutes(Integer.parseInt(property));
    } catch (NumberFormatException ignored) {
    }

    try {
      return Duration.parse(property);
    } catch (DateTimeParseException err) {
      SonarLintLogger.get().error("Timeout of system property '" + propertyName + "' cannot be parsed!", err);
      return null;
    }
  }
}
