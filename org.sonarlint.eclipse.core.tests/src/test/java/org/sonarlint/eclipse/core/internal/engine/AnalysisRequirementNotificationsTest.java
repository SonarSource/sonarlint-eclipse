/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2023 SonarSource SA
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonarlint.eclipse.core.SonarLintNotifications;
import org.sonarlint.eclipse.core.SonarLintNotifications.Notification;
import org.sonarlint.eclipse.core.internal.NotificationListener;
import org.sonarsource.sonarlint.core.analysis.api.AnalysisResults;
import org.sonarsource.sonarlint.core.analysis.api.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.PluginDetails;
import org.sonarsource.sonarlint.core.commons.Language;
import org.sonarsource.sonarlint.core.plugin.commons.SkipReason;
import org.sonarsource.sonarlint.core.plugin.commons.SkipReason.UnsatisfiedRuntimeRequirement.RuntimeRequirement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AnalysisRequirementNotificationsTest {

  private static final List<Notification> notifications = new ArrayList<>();
  private final AnalysisResults analysisResults = mock(AnalysisResults.class);
  Map<ClientInputFile, Language> detectedLang = new HashMap<>();

  @BeforeClass
  public static void prepare() throws Exception {
    SonarLintNotifications.get().addNotificationListener(new NotificationListener() {
      @Override
      public void showNotification(Notification notif) {
        notifications.add(notif);
      }
    });
  }

  @Before
  public void clear() {
    notifications.clear();
    AnalysisRequirementNotifications.resetCachedMessages();
    when(analysisResults.languagePerFile()).thenReturn(detectedLang);
  }

  @Test
  public void dontNotifyIfNoFilesAnalyzed() {
    AnalysisRequirementNotifications.notifyOnceForSkippedPlugins(analysisResults, Collections.emptyList());
    assertThat(notifications).isEmpty();
  }

  @Test
  public void dontNotifyIfNoLanguagesDetected() {
    detectedLang.put(mock(ClientInputFile.class), null);
    AnalysisRequirementNotifications.notifyOnceForSkippedPlugins(analysisResults, Collections.emptyList());
    assertThat(notifications).isEmpty();
  }

  @Test
  public void notifyIfSkippedLanguage_JRE() {
    detectedLang.put(mock(ClientInputFile.class), Language.JAVA);
    List<PluginDetails> plugins = List.of(new PluginDetails("java", "Java", "1.0", new SkipReason.UnsatisfiedRuntimeRequirement(RuntimeRequirement.JRE, "1.8", "11")));
    AnalysisRequirementNotifications.notifyOnceForSkippedPlugins(analysisResults, plugins);
    assertThat(notifications).usingFieldByFieldElementComparator()
      .containsOnly(
        new Notification(
          "Analyzer Requirement",
          "SonarLint failed to analyze Java code",
          "SonarLint requires Java runtime version 11 or later to analyze Java code. Current version is 1.8.\n" +
            "See <a href=\"https://wiki.eclipse.org/Eclipse.ini#Specifying_the_JVM\">the Eclipse Wiki</a> to configure your IDE to run with a more recent JRE."));
  }

  @Test
  public void notifyIfSkippedLanguage_Node() {
    detectedLang.put(mock(ClientInputFile.class), Language.JS);
    List<PluginDetails> plugins = List.of(
      new PluginDetails("javascript", "JS/TS", "1.0", new SkipReason.UnsatisfiedRuntimeRequirement(RuntimeRequirement.NODEJS, "7.2", "8.0")));
    AnalysisRequirementNotifications.notifyOnceForSkippedPlugins(analysisResults, plugins);
    assertThat(notifications).usingFieldByFieldElementComparator().containsOnly(
      new Notification(
        "Analyzer Requirement",
        "SonarLint failed to analyze JavaScript code",
        "SonarLint requires Node.js runtime version 8.0 or later to analyze JavaScript code. Current version is 7.2.\n" +
          "Please configure the Node.js path in the <a href=\"#edit-settings\">SonarLint settings</a>."));
  }

}
