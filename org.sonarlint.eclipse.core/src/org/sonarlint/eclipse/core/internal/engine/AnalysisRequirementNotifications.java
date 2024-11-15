/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.engine;

import java.util.HashSet;
import java.util.Set;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.SonarLintNotifications;
import org.sonarlint.eclipse.core.SonarLintNotifications.Notification;
import org.sonarsource.sonarlint.core.client.utils.Language;
import org.sonarsource.sonarlint.core.rpc.protocol.client.plugin.DidSkipLoadingPluginParams.SkipReason;

public class AnalysisRequirementNotifications {

  private static final Set<String> alreadyNotified = new HashSet<>();

  private AnalysisRequirementNotifications() {
    // NOP
  }

  public static void resetCachedMessages() {
    alreadyNotified.clear();
  }

  public static void notifyOnceForSkippedPlugins(org.sonarsource.sonarlint.core.rpc.protocol.common.Language analyzedSkippedLanguage, SkipReason skipReason, String minVersion,
    @Nullable String currentVersion) {
    var languageWithLabel = Language.valueOf(analyzedSkippedLanguage.name());
    final var shortMsg = "SonarQube for Eclipse failed to analyze " + languageWithLabel.getLabel() + " code";
    if (skipReason == SkipReason.UNSATISFIED_JRE) {
      var content = String.format(
        "SonarQube for Eclipse requires Java runtime version %s or later to analyze %s code. "
          + "Current version is %s.\n"
          + "See <a href=\"https://wiki.eclipse.org/Eclipse.ini#Specifying_the_JVM\">the Eclipse Wiki</a> to configure your IDE to run with a more recent JRE.",
        minVersion, languageWithLabel.getLabel(), currentVersion);
      createNotificationOnce(shortMsg, content);
    } else if (skipReason == SkipReason.UNSATISFIED_NODE_JS) {
      var content = new StringBuilder(
        String.format("SonarQube for Eclipse requires Node.js runtime version %s or later to analyze %s code.", minVersion, languageWithLabel.getLabel()));
      if (currentVersion != null) {
        content.append(String.format(" Current version is %s.", currentVersion));
      }
      content.append("\nPlease configure the Node.js path in the <a href=\"#edit-settings\">SonarQube settings</a>.");
      createNotificationOnce(shortMsg, content.toString());
    }
  }

  private static void createNotificationOnce(String shortMsg, String longMsg) {
    if (!alreadyNotified.contains(longMsg)) {
      SonarLintNotifications.get().showNotification(new Notification("Analyzer Requirement", shortMsg, longMsg));
      alreadyNotified.add(longMsg);
    }
  }
}
