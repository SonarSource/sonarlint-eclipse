/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2020 SonarSource SA
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.assertj.core.groups.Tuple;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.jobs.LogListener;
import org.sonarsource.sonarlint.core.client.api.common.PluginDetails;
import org.sonarsource.sonarlint.core.client.api.common.SkipReason;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class SkippedPluginsNotifierTest {

  private static final List<Tuple> notifications = new ArrayList<>();

  @BeforeClass
  public static void prepare() throws Exception {
    SonarLintLogger.get().addLogListener(new LogListener() {
      @Override
      public void info(String msg, boolean fromAnalyzer) {
      }

      @Override
      public void error(String msg, boolean fromAnalyzer) {
      }

      @Override
      public void debug(String msg, boolean fromAnalyzer) {
      }

      @Override
      public void showNotification(String title, String shortMsg, String longMsg) {
        notifications.add(Tuple.tuple(title, shortMsg, longMsg));
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
    List<PluginDetails> plugins = Arrays.asList(new FakePluginDetails("plugin1", "Plugin 1", "1.0", null));
    SkippedPluginsNotifier.notifyForSkippedPlugins(plugins, null);
    assertThat(notifications).isEmpty();
  }

  @Test
  public void notifyIfSkippedPlugin_JRE() {
    List<PluginDetails> plugins = Arrays.asList(new FakePluginDetails("plugin1", "Plugin 1", "1.0", new SkipReason.UnsatisfiedJreRequirement("1.8", "11")));
    SkippedPluginsNotifier.notifyForSkippedPlugins(plugins, null);
    assertThat(notifications).containsOnly(tuple("Rules not available", "Some rules are not available until some requirements are satisfied",
      "Some analyzers can not be loaded.\n\n"
        + " - 'Plugin 1' requires Java runtime version 11 or later. Current version is 1.8.\n\n"
        + "Please see <a href=\"https://wiki.eclipse.org/Eclipse.ini#Specifying_the_JVM\">the Eclipse Wiki</a> to configure your IDE to run with a more recent JRE."));
  }

  @Test
  public void notifyIfSkippedLanguage_JRE() {
    List<PluginDetails> plugins = Arrays.asList(new FakePluginDetails("java", "Java", "1.0", new SkipReason.UnsatisfiedJreRequirement("1.8", "11")));
    SkippedPluginsNotifier.notifyForSkippedPlugins(plugins, null);
    assertThat(notifications).containsOnly(tuple("Language analysis not available", "Java analysis will not be available until some requirements are satisfied",
      "Some analyzers can not be loaded.\n\n"
        + "Java analysis will not be available until following requirements are satisfied:\n"
        + " - 'Java' requires Java runtime version 11 or later. Current version is 1.8.\n\n"
        + "Please see <a href=\"https://wiki.eclipse.org/Eclipse.ini#Specifying_the_JVM\">the Eclipse Wiki</a> to configure your IDE to run with a more recent JRE."));
  }

  @Test
  public void notifyIfSkippedLanguages_JRE() {
    List<PluginDetails> plugins = Arrays.asList(new FakePluginDetails("java", "Java", "1.0", new SkipReason.UnsatisfiedJreRequirement("1.8", "11")),
      new FakePluginDetails("cpp", "CFamily", "1.0", new SkipReason.UnsatisfiedJreRequirement("1.8", "11")));
    SkippedPluginsNotifier.notifyForSkippedPlugins(plugins, null);
    assertThat(notifications).containsOnly(tuple("Language analysis not available", "Java, C, Cpp analysis will not be available until some requirements are satisfied",
      "Some analyzers can not be loaded.\n\n"
        + "Java, C, Cpp analysis will not be available until following requirements are satisfied:\n"
        + " - 'Java' requires Java runtime version 11 or later. Current version is 1.8.\n"
        + " - 'CFamily' requires Java runtime version 11 or later. Current version is 1.8.\n\n"
        + "Please see <a href=\"https://wiki.eclipse.org/Eclipse.ini#Specifying_the_JVM\">the Eclipse Wiki</a> to configure your IDE to run with a more recent JRE."));
  }

  @Test
  public void notifyIfSkippedPlugin_IncompatiblePluginApi() {
    List<PluginDetails> plugins = Arrays.asList(new FakePluginDetails("plugin1", "Plugin 1", "1.0", SkipReason.IncompatiblePluginApi.INSTANCE));
    SkippedPluginsNotifier.notifyForSkippedPlugins(plugins, null);
    assertThat(notifications).containsOnly(tuple("Rules not available", "Some rules are not available until some requirements are satisfied",
      "Some analyzers can not be loaded.\n\n"
        + " - 'Plugin 1' is not compatible with this version of SonarLint. Ensure you are using the latest version of SonarLint and check SonarLint output for details.\n"));
  }

  @Test
  public void notifyIfSkippedPlugin_UnsatisfiedDependency() {
    List<PluginDetails> plugins = Arrays.asList(new FakePluginDetails("plugin1", "Plugin 1", "1.0", new SkipReason.UnsatisfiedDependency("java")));
    SkippedPluginsNotifier.notifyForSkippedPlugins(plugins, "mySq");
    assertThat(notifications).containsOnly(tuple("Rules not available", "Some rules are not available until some requirements are satisfied",
      "Some analyzers from connection 'mySq' can not be loaded.\n\n"
        + " - 'Plugin 1' is missing dependency 'java'\n"));
  }

  @Test
  public void notifyIfSkippedPlugin_IncompatiblePluginVersion() {
    List<PluginDetails> plugins = Arrays.asList(new FakePluginDetails("plugin1", "Plugin 1", "1.0", new SkipReason.IncompatiblePluginVersion("2.0")));
    SkippedPluginsNotifier.notifyForSkippedPlugins(plugins, "mySq");
    assertThat(notifications).containsOnly(tuple("Rules not available", "Some rules are not available until some requirements are satisfied",
      "Some analyzers from connection 'mySq' can not be loaded.\n\n"
        + " - 'Plugin 1' is too old for SonarLint. Current version is 1.0. Minimal supported version is 2.0. Please update your binding.\n"));
  }

  private static class FakePluginDetails implements PluginDetails {

    private String key;
    private String name;
    private String version;
    private @Nullable SkipReason skipReason;

    public FakePluginDetails(String key, String name, String version, @Nullable SkipReason skipReason) {
      this.key = key;
      this.name = name;
      this.version = version;
      this.skipReason = skipReason;
    }

    @Override
    public String key() {
      return key;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public String version() {
      return version;
    }

    @Override
    public Optional<SkipReason> skipReason() {
      return Optional.ofNullable(skipReason);
    }

  }

}
