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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonarlint.eclipse.core.SonarLintNotifications;
import org.sonarlint.eclipse.core.SonarLintNotifications.Notification;
import org.sonarlint.eclipse.core.internal.NotificationListener;
import org.sonarlint.eclipse.core.internal.engine.SkippedPluginsNotifier;
import org.sonarsource.sonarlint.core.client.api.common.PluginDetails;
import org.sonarsource.sonarlint.core.plugin.commons.SkipReason;

import static org.assertj.core.api.Assertions.assertThat;

public class SkippedPluginsNotifierTest {

  private static final List<Notification> notifications = new ArrayList<>();

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
  }

  @Test
  public void dontNotifyIfNoPlugins() {
    SkippedPluginsNotifier.notifyForSkippedPlugins(Collections.emptyList(), null);
    assertThat(notifications).isEmpty();
  }

  @Test
  public void dontNotifyIfNoSkippedPlugins() {
    List<PluginDetails> plugins = List.of(new PluginDetails("plugin1", "Plugin 1", "1.0", null));
    SkippedPluginsNotifier.notifyForSkippedPlugins(plugins, null);
    assertThat(notifications).isEmpty();
  }

  @Test
  public void notifyIfSkippedPlugin_IncompatiblePluginApi() {
    List<PluginDetails> plugins = List.of(new PluginDetails("plugin1", "Plugin 1", "1.0", SkipReason.IncompatiblePluginApi.INSTANCE));
    SkippedPluginsNotifier.notifyForSkippedPlugins(plugins, null);
    assertThat(notifications).usingFieldByFieldElementComparator()
      .containsOnly(
        new Notification("Rules not available",
          "Some rules are not available until some requirements are satisfied",
          "Some analyzers can not be loaded.\n\n"
            + " - 'Plugin 1' is not compatible with this version of SonarLint. Ensure you are using the latest version of SonarLint and check SonarLint output for details."
            + System.lineSeparator()));
  }

  @Test
  public void notifyIfSkippedPlugin_UnsatisfiedDependency() {
    List<PluginDetails> plugins = List.of(new PluginDetails("plugin1", "Plugin 1", "1.0", new SkipReason.UnsatisfiedDependency("java")));
    SkippedPluginsNotifier.notifyForSkippedPlugins(plugins, "mySq");
    assertThat(notifications).usingFieldByFieldElementComparator()
      .containsOnly(
        new Notification("Rules not available",
          "Some rules are not available until some requirements are satisfied",
          "Some analyzers from connection 'mySq' can not be loaded.\n\n"
            + " - 'Plugin 1' is missing dependency 'java'" + System.lineSeparator()));
  }

  @Test
  public void notifyIfSkippedPlugin_IncompatiblePluginVersion() {
    List<PluginDetails> plugins = List.of(new PluginDetails("plugin1", "Plugin 1", "1.0", new SkipReason.IncompatiblePluginVersion("2.0")));
    SkippedPluginsNotifier.notifyForSkippedPlugins(plugins, "mySq");
    assertThat(notifications).usingFieldByFieldElementComparator()
      .containsOnly(
        new Notification("Rules not available",
          "Some rules are not available until some requirements are satisfied",
          "Some analyzers from connection 'mySq' can not be loaded.\n\n"
            + " - 'Plugin 1' is too old for SonarLint. Current version is 1.0. Minimal supported version is 2.0. Please update your binding." + System.lineSeparator()));
  }

}
