/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2022 SonarSource SA
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.sonarlint.eclipse.core.SonarLintNotifications;
import org.sonarlint.eclipse.core.SonarLintNotifications.Notification;
import org.sonarsource.sonarlint.core.analysis.api.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.PluginDetails;
import org.sonarsource.sonarlint.core.plugin.commons.SkipReason;

import static java.util.stream.Collectors.toSet;

public class AnalysisRequirementNotifications {

  private static final Set<String> alreadyNotified = new HashSet<>();

  private AnalysisRequirementNotifications() {
    // NOP
  }

  public static void resetCachedMessages() {
    alreadyNotified.clear();
  }

  public static void notifyOnceForSkippedPlugins(AnalysisResults analysisResults, Collection<PluginDetails> allPlugins) {
    var attemptedLanguages = analysisResults.languagePerFile().values()
      .stream()
      .filter(Objects::nonNull)
      .collect(toSet());
    attemptedLanguages.forEach(l -> {
      final var correspondingPlugin = allPlugins.stream().filter(p -> p.key().equals(l.getPluginKey())).findFirst();
      correspondingPlugin.flatMap(PluginDetails::skipReason).ifPresent(skipReason -> {
        if (skipReason instanceof SkipReason.UnsatisfiedRuntimeRequirement) {
          final var runtimeRequirement = (SkipReason.UnsatisfiedRuntimeRequirement) skipReason;
          final var shortMsg = "SonarLint failed to analyze " + l.getLabel() + " code";
          if (runtimeRequirement.getRuntime() == SkipReason.UnsatisfiedRuntimeRequirement.RuntimeRequirement.JRE) {
            var content = String.format(
              "SonarLint requires Java runtime version %s or later to analyze %s code. "
                + "Current version is %s.\n"
                + "See <a href=\"https://wiki.eclipse.org/Eclipse.ini#Specifying_the_JVM\">the Eclipse Wiki</a> to configure your IDE to run with a more recent JRE.",
              runtimeRequirement.getMinVersion(), l.getLabel(), runtimeRequirement.getCurrentVersion());
            createNotificationOnce(shortMsg, content);
          } else if (runtimeRequirement.getRuntime() == SkipReason.UnsatisfiedRuntimeRequirement.RuntimeRequirement.NODEJS) {
            var content = new StringBuilder(
              String.format("SonarLint requires Node.js runtime version %s or later to analyze %s code.", runtimeRequirement.getMinVersion(), l.getLabel()));
            if (runtimeRequirement.getCurrentVersion() != null) {
              content.append(String.format(" Current version is %s.", runtimeRequirement.getCurrentVersion()));
            }
            content.append("\nPlease configure the Node.js path in the <a href=\"#edit-settings\">SonarLint settings</a>.");
            createNotificationOnce(shortMsg, content.toString());
          }
        }
      });
    });
  }

  private static void createNotificationOnce(String shortMsg, String longMsg) {
    if (!alreadyNotified.contains(longMsg)) {
      SonarLintNotifications.get().showNotification(new Notification("Analyzer Requirement", shortMsg, longMsg));
      alreadyNotified.add(longMsg);
    }
  }
}
