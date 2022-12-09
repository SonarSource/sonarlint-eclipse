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
package org.sonarlint.eclipse.core.internal.backend;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;

import static java.util.stream.Collectors.toList;

public class PluginPathHelper {

  public static List<Path> getEmbeddedPluginPaths() {
    var pluginEntriesEnum = SonarLintCorePlugin.getInstance().getBundle().findEntries("/plugins", "*.jar", false);
    if (pluginEntriesEnum != null) {
      return Collections.list(pluginEntriesEnum).stream()
        .map(PluginPathHelper::toPath)
        .filter(Objects::nonNull)
        .collect(toList());
    } else {
      throw new IllegalStateException("Unable to find any embedded plugin");
    }
  }

  @Nullable
  public static Path toPath(URL bundleEntry) {
    try {
      var localURL = FileLocator.toFileURL(bundleEntry);
      SonarLintLogger.get().debug("Plugin extracted to " + localURL);
      return new File(localURL.getFile()).toPath();
    } catch (Exception e) {
      SonarLintLogger.get().error("Unable to load plugin " + bundleEntry, e);
      return null;
    }
  }

  @Nullable
  public static Path findEmbeddedSecretsPlugin() {
    return findEmbeddedPlugin("sonar-secrets-plugin-*.jar", "Found Secrets detection plugin: ");
  }

  @Nullable
  public static Path findEmbeddedJsPlugin() {
    return findEmbeddedPlugin("sonar-javascript-plugin-*.jar", "Found JS/TS plugin: ");
  }

  @Nullable
  public static Path findEmbeddedHtmlPlugin() {
    return findEmbeddedPlugin("sonar-html-plugin-*.jar", "Found HTML plugin: ");
  }

  @Nullable
  public static Path findEmbeddedXmlPlugin() {
    return findEmbeddedPlugin("sonar-xml-plugin-*.jar", "Found XML plugin: ");
  }

  @Nullable
  private static Path findEmbeddedPlugin(String pluginNamePattern, String logPrefix) {
    var pluginEntriesEnum = SonarLintCorePlugin.getInstance().getBundle()
      .findEntries("/plugins", pluginNamePattern, false);
    if (pluginEntriesEnum == null) {
      return null;
    }
    var pluginUrls = Collections.list(pluginEntriesEnum);
    pluginUrls.forEach(pluginUrl -> SonarLintLogger.get().debug(logPrefix + pluginUrl));
    if (pluginUrls.size() > 1) {
      throw new IllegalStateException("Multiple plugins found");
    }
    return pluginUrls.size() == 1 ? toPath(pluginUrls.get(0)) : null;
  }

}
