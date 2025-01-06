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
package org.sonarlint.eclipse.core.internal.engine;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonarlint.eclipse.core.SonarLintNotifications;
import org.sonarlint.eclipse.core.SonarLintNotifications.Notification;
import org.sonarlint.eclipse.core.internal.NotificationListener;
import org.sonarsource.sonarlint.core.rpc.protocol.client.plugin.DidSkipLoadingPluginParams.SkipReason;
import org.sonarsource.sonarlint.core.rpc.protocol.common.Language;

import static org.assertj.core.api.Assertions.assertThat;

public class AnalysisRequirementNotificationsTest {

  private static final List<Notification> notifications = new ArrayList<>();

  @BeforeClass
  public static void prepare() {
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
  }

  @Test
  public void notifyIfSkippedLanguage_JRE() {
    AnalysisRequirementNotifications.notifyOnceForSkippedPlugins(Language.JAVA, SkipReason.UNSATISFIED_JRE, "11", "1.8");
    assertThat(notifications).usingRecursiveComparison()
      .isEqualTo(
        List.of(new Notification(
          "Analyzer Requirement",
          "SonarQube for Eclipse failed to analyze Java code",
          "SonarQube for Eclipse requires Java runtime version 11 or later to analyze Java code. Current version is 1.8.\n" +
            "See <a href=\"https://wiki.eclipse.org/Eclipse.ini#Specifying_the_JVM\">the Eclipse Wiki</a> to configure your IDE to run with a more recent JRE.")));
  }

  @Test
  public void notifyIfSkippedLanguage_Node() {
    AnalysisRequirementNotifications.notifyOnceForSkippedPlugins(Language.JS, SkipReason.UNSATISFIED_NODE_JS, "8.0", "7.2");
    assertThat(notifications).usingRecursiveComparison().isEqualTo(
      List.of(new Notification(
        "Analyzer Requirement",
        "SonarQube for Eclipse failed to analyze JavaScript code",
        "SonarQube for Eclipse requires Node.js runtime version 8.0 or later to analyze JavaScript code. Current version is 7.2.\n" +
          "Please configure the Node.js path in the <a href=\"#edit-settings\">SonarQube settings</a>.")));
  }

}
