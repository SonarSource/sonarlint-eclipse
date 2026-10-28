/*
 * SonarLint for Eclipse
 * Copyright (C) SonarSource Sàrl
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
package org.sonarlint.eclipse.core.internal.preferences;

import org.eclipse.core.runtime.preferences.InstanceScope;

/**
 * Verbose logging preference is stored in the UI plug-in preference node; core reads it without a bundle dependency.
 */
public final class VerboseLoggingPreferences {

  /** Must match {@code org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin#PLUGIN_ID}. */
  public static final String UI_PLUGIN_ID = "org.sonarlint.eclipse.ui";

  /** Must match {@code org.sonarlint.eclipse.ui.internal.console.SonarLintConsole#P_VERBOSE_OUTPUT}. */
  public static final String VERBOSE_OUTPUT_KEY = "debugOutput";

  private VerboseLoggingPreferences() {
    // utility
  }

  public static boolean isVerboseEnabled() {
    return InstanceScope.INSTANCE.getNode(UI_PLUGIN_ID).getBoolean(VERBOSE_OUTPUT_KEY, false);
  }
}
